package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.support.ExchangeSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.LOAD_BALANCING__FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/5/8
 */
@Component
public class LoadBalancingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancingFilter.class);


    @Override
    public int getOrder() {
        return LOAD_BALANCING__FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);

        String routeHost = ExchangeSupport.getRouteRequestPath(exchange);
//        logger.info("请求地址 : {},转发服务地址 : {}", cacheRequestEntity.getRequestLine(), routeHost);
        cacheRequestEntity.setRealServer(routeHost);
        ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);

        return chain.filter(exchange);
    }

}
