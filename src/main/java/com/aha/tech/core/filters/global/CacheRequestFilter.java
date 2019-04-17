package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.List;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.PRE_HANDLER_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 预处理过滤器
 */
@Component
public class CacheRequestFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CacheRequestFilter.class);

    private static final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();


    @Override
    public int getOrder() {
        return PRE_HANDLER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        CacheRequestEntity cacheRequestEntity = new CacheRequestEntity();
        cacheRequestEntity.setPath(exchange.getRequest().getURI().toString());

        if (exchange.getRequest().getMethod().equals(HttpMethod.POST)) {
            return readBody(exchange, chain, cacheRequestEntity);
        }

        return chain.filter(exchange);
    }

    /**
     * 缓存body
     * @param exchange
     * @param chain
     * @param cacheRequestEntity
     * @return
     */
    private Mono<Void> readBody(ServerWebExchange exchange, GatewayFilterChain chain, CacheRequestEntity cacheRequestEntity) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    DataBufferUtils.retain(dataBuffer);
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    String s = new String(content, Charset.forName("UTF-8"));
                    cacheRequestEntity.setData(s);
                    exchange.getAttributes().put(GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR, cacheRequestEntity);
                    return chain.filter(exchange);
                });
    }
}
