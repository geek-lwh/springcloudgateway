//package com.aha.tech.core.filters.global;
//
//import com.aha.tech.core.model.entity.CacheRequestEntity;
//import com.aha.tech.core.support.ExchangeSupport;
//import com.aha.tech.util.LogUtil;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//import static com.aha.tech.core.constant.FilterProcessOrderedConstant.LOAD_BALANCING_FILTER;
//import static com.aha.tech.core.constant.HeaderFieldConstant.REQUEST_ID;
//import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
//
///**
// * @Author: luweihong
// * @Date: 2019/5/8
// */
//@Component
//public class LoadBalancingFilter implements GlobalFilter, Ordered {
//
//    private static final Logger logger = LoggerFactory.getLogger(LoadBalancingFilter.class);
//
//    @Override
//    public int getOrder() {
//        return LOAD_BALANCING_FILTER;
//    }
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // 从namingService 获取所有instance,获取instance的weight
//        LogUtil.combineTraceId(exchange);
//
////        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
////        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
////        String routeHost = ExchangeSupport.getRouteRequestPath(exchange);
////        cacheRequestEntity.setRealServer(routeHost);
////        ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);
////        logger.info("route info : {} ", cacheRequestEntity);
//        return chain.filter(exchange);
//    }
//
//
//}
