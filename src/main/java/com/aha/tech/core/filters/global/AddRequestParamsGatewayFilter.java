package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.FilterOrdered;
import com.aha.tech.core.handler.SessionHandler;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
@Component
public class AddRequestParamsGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AddRequestParamsGatewayFilter.class);

    @Override
    public int getOrder() {
        return FilterOrdered.GLOBAL_ADD_REQUEST_PARAMS_GATEWAY_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行添加get参数过滤器");
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpMethod httpMethod = serverHttpRequest.getMethod();
        URI uri = serverHttpRequest.getURI();

        UserVo userVo = SessionHandler.get();
        if (httpMethod != HttpMethod.GET || userVo == null) {
            logger.info("不满足 执行添加get参数过滤器 要求,url : {},httpMethod : {} ", uri, httpMethod);
            return chain.filter(exchange);
        }


        String originalQuery = uri.getRawQuery();

        StringBuilder query = new StringBuilder();
        if (org.springframework.util.StringUtils.hasText(originalQuery)) {
            query.append(originalQuery);
            if (originalQuery.charAt(originalQuery.length() - 1) != '&') {
                query.append('&');
            }
        }


        query.append("user_id").append("=").append(userVo.getUserId());

        URI newUri = UriComponentsBuilder.fromUri(uri)
                .replaceQuery(query.toString())
                .build(false)
                .toUri();

        ServerHttpRequest newRequest = serverHttpRequest.mutate().uri(newUri).build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

}
