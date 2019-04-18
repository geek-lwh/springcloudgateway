package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.entity.TamperProofEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.WriteResponseSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.CACHE_REQUEST_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 预处理过滤器
 */
@Component
public class CacheRequestFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CacheRequestFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Value("${url.tamper.proof.enable:false}")
    private boolean isEnable;

    @Override
    public int getOrder() {
        return CACHE_REQUEST_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        CacheRequestEntity cacheRequestEntity = new CacheRequestEntity();
        exchange.getAttributes().put(GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR, cacheRequestEntity);

        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        HttpHeaders httpHeaders = request.getHeaders();

        cacheRequestEntity.setRequestLine(uri);

        TamperProofEntity tamperProofEntity = new TamperProofEntity(httpHeaders);
        boolean isURIValid = checkUrlValid(tamperProofEntity, uri);
        if (!isURIValid) {
            return Mono.defer(() -> {
                logger.error("url防篡改校验失败 参数 : {}", tamperProofEntity);
                ResponseVo rpcResponse = new ResponseVo(HttpStatus.FORBIDDEN.value(), "url防篡改校验失败");
                return WriteResponseSupport.write(exchange, rpcResponse, HttpStatus.FORBIDDEN);
            });
        }

        return checkAndCache(exchange, chain, cacheRequestEntity, tamperProofEntity);
    }

    /**
     * 校验url
     * @param tamperProofEntity
     * @param uri
     * @return
     */
    private boolean checkUrlValid(TamperProofEntity tamperProofEntity, URI uri) {
        if (!isEnable) {
            return Boolean.TRUE;
        }

        String timestamp = tamperProofEntity.getTimestamp();
        String signature = tamperProofEntity.getSignature();
        String version = tamperProofEntity.getVersion();

        return httpRequestHandlerService.urlTamperProof(version, timestamp, signature, uri);
    }

    /**
     * 获取body
     * @param exchange
     * @param chain
     * @param cacheRequestEntity
     * @return
     */
    private Mono<Void> checkAndCache(ServerWebExchange exchange, GatewayFilterChain chain, CacheRequestEntity cacheRequestEntity, TamperProofEntity tamperProofEntity) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    DataBufferUtils.retain(dataBuffer);
                    byte[] buf = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(buf);
                    String data = new String(buf, StandardCharsets.UTF_8);
                    if (!checkBodyValid(data, tamperProofEntity)) {
                        return Mono.defer(() -> {
                            logger.error("body防篡改校验失败 参数: {}", tamperProofEntity);
                            ResponseVo rpcResponse = new ResponseVo(HttpStatus.FORBIDDEN.value(), "body防篡改校验失败");
                            return WriteResponseSupport.write(exchange, rpcResponse, HttpStatus.FORBIDDEN);
                        });
                    }
                    cacheRequestEntity.setRequestBody(data);
                    return chain.filter(exchange);
                });
    }

    /**
     * 检查body是否合法
     * @param data
     * @param tamperProofEntity
     * @return
     */
    private Boolean checkBodyValid(String data, TamperProofEntity tamperProofEntity) {
        if (isEnable) {
            String version = tamperProofEntity.getVersion();
            String content = tamperProofEntity.getContent();
            String timestamp = tamperProofEntity.getTimestamp();

            return httpRequestHandlerService.bodyTamperProof(version, data, timestamp, content);
        }

        return Boolean.TRUE;
    }
}
