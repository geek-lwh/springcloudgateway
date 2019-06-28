package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.support.ExchangeSupport;
import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.naming.NamingService;
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

    @NacosInjected
    private NamingService namingService;

    @Override
    public int getOrder() {
        return LOAD_BALANCING__FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        URI r = (URI) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_URL_ATTR, URI.create(""));
//        if(StringUtils.isBlank(r.getRawPath())){
//            // 没找到对应的uri
//        }

        // 从namingService 获取所有instance,获取instance的weight

        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        String routeHost = ExchangeSupport.getRouteRequestPath(exchange);
        logger.debug("请求地址 : {},转发服务地址 : {}", cacheRequestEntity.getRequestLine(), routeHost);
        cacheRequestEntity.setRealServer(routeHost);
        ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);
//        namingService.getAllInstances("",);
        return chain.filter(exchange);
    }


}
