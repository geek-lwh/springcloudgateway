package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.support.ExchangeSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.COPY_BODY_FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 预处理过滤器
 */
@Component
public class CopyBodyFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CopyBodyFilter.class);

    @Override
    public int getOrder() {
        return COPY_BODY_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders httpHeaders = request.getHeaders();
        HttpMethod httpMethod = request.getMethod();
        if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
            return DataBufferUtils.join(request.getBody())
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return bytes;
                    })
                    .defaultIfEmpty(new byte[0])
                    .doOnNext(bytes -> {
                        String body = new String(bytes, StandardCharsets.UTF_8);
                        logger.debug("原始 body : {} ", body);
                        CacheRequestEntity cacheRequestEntity = new CacheRequestEntity();
                        cacheRequestEntity.setRequestBody(body);
                        cacheRequestEntity.setRequestLine(exchange.getRequest().getURI());
                        ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR, cacheRequestEntity);
                    }).then(chain.filter(exchange));
        }

        return chain.filter(exchange);
    }


}
