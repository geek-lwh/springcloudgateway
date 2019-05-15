package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.entity.TamperProofEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.WriteResponseSupport;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.BODY_TAMPER_PROOF_FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 预处理过滤器
 */
@Component
public class BodyTamperProofRequestFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(BodyTamperProofRequestFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Value("${gateway.tamper.proof.enable:false}")
    private boolean isEnable;

    @Override
    public int getOrder() {
        return BODY_TAMPER_PROOF_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        CacheRequestEntity cacheRequestEntity = new CacheRequestEntity();
        ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR, cacheRequestEntity);

        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        cacheRequestEntity.setRequestLine(uri);
        HttpHeaders httpHeaders = request.getHeaders();
        TamperProofEntity tamperProofEntity = new TamperProofEntity(httpHeaders, uri);

        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        if (mediaType != null && mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
            return chain.filter(exchange);
        }

        HttpMethod httpMethod = request.getMethod();
        if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        DataBufferUtils.retain(dataBuffer);
                        byte[] buf = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(buf);
                        String data = new String(buf, StandardCharsets.UTF_8);
                        String body = JSON.parseObject(data).toJSONString();
                        cacheRequestEntity.setRequestBody(body);
                        if (!bodyTamperProof(data, tamperProofEntity)) {
                            return Mono.defer(() -> {
                                String errorMsg = String.format("body 防篡改校验失败,参数:%s", tamperProofEntity);
                                ResponseVo rpcResponse = new ResponseVo(HttpStatus.FORBIDDEN.value(), errorMsg);
                                return WriteResponseSupport.shortCircuit(exchange, rpcResponse, HttpStatus.FORBIDDEN, errorMsg);
                            });
                        }

                        return chain.filter(exchange);
                    });
        }

        return chain.filter(exchange);
    }

    /**
     * 检查body是否合法
     * @param body
     * @param tamperProofEntity
     * @return
     */
    private Boolean bodyTamperProof(String body, TamperProofEntity tamperProofEntity) {
        if (isEnable) {
            logger.debug("接收到的原始body : {}", body);

            String version = tamperProofEntity.getVersion();
            String content = tamperProofEntity.getContent();
            String timestamp = tamperProofEntity.getTimestamp();
            logger.debug("加密信息 : {}", tamperProofEntity);
            return httpRequestHandlerService.bodyTamperProof(version, body, timestamp, content);
        }

        return Boolean.TRUE;
    }
}