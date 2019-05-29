package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.entity.TamperProofEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheBody(exchange);

        ServerHttpRequest request = exchange.getRequest();
        HttpMethod httpMethod = request.getMethod();
        if (!httpMethod.equals(HttpMethod.POST)) {
            return chain.filter(exchange);
        }
        URI uri = request.getURI();
        Boolean isSkipUrlTamperProof = ExchangeSupport.getIsSkipUrlTamperProof(exchange);
        if (isSkipUrlTamperProof) {
            logger.info("跳过body防篡改,raw_path : {}", uri.getRawPath());
            return chain.filter(exchange);
        }

        HttpHeaders httpHeaders = request.getHeaders();
        MediaType mediaType = httpHeaders.getContentType();
        if (mediaType != null && mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
            logger.info("media type不是json : {}", mediaType);
            return chain.filter(exchange);
        }

        TamperProofEntity tamperProofEntity = new TamperProofEntity(httpHeaders, uri);
        String body = cacheRequestEntity.getRequestBody();
        if (!bodyTamperProof(body, tamperProofEntity)) {
            return Mono.defer(() -> {
                String errorMsg = String.format("body : %s 防篡改校验失败,参数:%s", body, tamperProofEntity);
                ResponseVo rpcResponse = new ResponseVo(HttpStatus.FORBIDDEN.value(), errorMsg);
                return ResponseSupport.write(exchange, rpcResponse);
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
            String version = tamperProofEntity.getVersion();
            String content = tamperProofEntity.getContent();
            String timestamp = tamperProofEntity.getTimestamp();
            return httpRequestHandlerService.bodyTamperProof(version, body, timestamp, content);
        }

        return Boolean.TRUE;
    }
}
