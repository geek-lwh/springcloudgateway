package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.support.ExchangeSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.LOAD_BALANCING_FILTER;
import static com.aha.tech.core.constant.HeaderFieldConstant.REQUEST_ID;
import static com.aha.tech.core.constant.HeaderFieldConstant.X_TRACE_ID;

/**
 * @Author: luweihong
 * @Date: 2019/5/8
 */
@Component
public class LoadBalancingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancingFilter.class);

    @Override
    public int getOrder() {
        return LOAD_BALANCING_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 从namingService 获取所有instance,获取instance的weight

        List<String> clientRequestId = exchange.getRequest().getHeaders().get(REQUEST_ID);
        if (!CollectionUtils.isEmpty(clientRequestId)) {
            MDC.put(X_TRACE_ID, clientRequestId.get(0));
        }

        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        String routeHost = ExchangeSupport.getRouteRequestPath(exchange);
        logger.info("请求信息 : {} ", cacheRequestEntity);
        cacheRequestEntity.setRealServer(routeHost);
        ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);
        return chain.filter(exchange);
    }


}
