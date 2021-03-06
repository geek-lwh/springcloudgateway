package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.FilterProcessOrderedConstant;
import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;


/**
 * @Author: luweihong
 * @Date: 2019/2/20
 *
 * 重写请求路径网关过滤器
 */
@Component
public class RewritePathFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RewritePathFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return FilterProcessOrderedConstant.REWRITE_REQUEST_PATH_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest newRequest = replacePath(exchange);
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 重写path
     * @param exchange
     * @return
     */
    private ServerHttpRequest replacePath(ServerWebExchange exchange) {
        ServerHttpRequest newRequest = httpRequestHandlerService.rewriteRequestPath(exchange);
        return newRequest;
    }

}
