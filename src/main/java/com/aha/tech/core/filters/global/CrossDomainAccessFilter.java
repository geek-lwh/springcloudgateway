package com.aha.tech.core.filters.global;

import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.service.RequestHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.CROSS_DOMAIN_ACCESS_FILTER_ORDER;
import static com.aha.tech.core.support.WriteResponseSupport.writeError;

/**
 * @Author: monkey
 * @Date: 2019/3/30
 */
@Component
public class CrossDomainAccessFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CrossDomainAccessFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;


    @Override
    public int getOrder() {
        return CROSS_DOMAIN_ACCESS_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行跨域访问过滤器");

        try {
            httpRequestHandlerService.crossDomainAccessSetting(exchange);
        } catch (GatewayException e) {
            return Mono.defer(() -> writeError(exchange, e));
        }

        return chain.filter(exchange);
    }

}
