package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.SnapshotRequestEntity;
import com.aha.tech.core.support.AttributeSupport;
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

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.SNAPSHOT_FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 快照过滤器
 * 用于对请求快照拷贝
 */
@Component
public class SnapshotFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotFilter.class);

    @Override
    public int getOrder() {
        return SNAPSHOT_FILTER;
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

        SnapshotRequestEntity snapshotRequestEntity = AttributeSupport.getSnapshotRequest(exchange);

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
                        snapshotRequestEntity.setRequestBody(new String(bytes, StandardCharsets.UTF_8).trim());
                    }).then(chain.filter(exchange));
        }

        return chain.filter(exchange);
    }


}
