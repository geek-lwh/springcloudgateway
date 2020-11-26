package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.constant.AttributeConstant.GATEWAY_REQUEST_CACHED_ATTR;
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
        LogUtil.combineTraceId(exchange);
        return readFromStream(exchange, chain);
    }

    /**
     * 读取流
     * @param exchange
     * @param chain
     * @return
     */
    private Mono<Void> readFromStream(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod httpMethod = request.getMethod();

        CacheRequestEntity cacheRequestEntity = new CacheRequestEntity();
        cacheRequestEntity.setRequestLine(exchange.getRequest().getURI());
        cacheRequestEntity.setOriginalRequestHttpHeaders(request.getHeaders());
        ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);

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
                        String body = new String(bytes, StandardCharsets.UTF_8).trim();
                        logger.debug("原始 body : {} ", body);
                        cacheRequestEntity.setRequestBody(body);
                    }).then(chain.filter(exchange));
        }

        return chain.filter(exchange);
    }


}
