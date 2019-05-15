package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.AuthorizationService;
import com.aha.tech.core.support.ExchangeSupport;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.IS_AUTH_WHITE_LIST_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.WHITE_LIST_REQUEST_FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/5/8
 */
@Component
public class WhiteListFilter implements GlobalFilter, Ordered {

    @Resource
    private AuthorizationService httpAuthorizationService;

    @Override
    public int getOrder() {
        return WHITE_LIST_REQUEST_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        // 是否跳过授权
        Boolean isSkipAuth = httpAuthorizationService.isSkipAuth(rawPath);

        ExchangeSupport.put(exchange, IS_AUTH_WHITE_LIST_ATTR, isSkipAuth);

        return chain.filter(exchange);
    }

}
