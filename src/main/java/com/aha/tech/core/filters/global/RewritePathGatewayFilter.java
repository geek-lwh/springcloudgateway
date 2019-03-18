package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class RewritePathGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RewritePathGatewayFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return FilterOrderedConstant.GLOBAL_REWRITE_REQUEST_PATH_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("进入重写请求路径网关过滤器");

        ServerHttpRequest newRequest = httpRequestHandlerService.rewriteRequestPath(exchange);

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

}
