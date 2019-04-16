package com.aha.tech.core.filters.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/4/10
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CrossDomainFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        ServerHttpRequest request = serverWebExchange.getRequest();
        if (CorsUtils.isCorsRequest(request)) {
            if (request.getMethod() == HttpMethod.OPTIONS) {
                ServerHttpResponse response = serverWebExchange.getResponse();
                HttpHeaders httpHeaders = response.getHeaders();
                httpHeaders.setAccessControlAllowOrigin(HEADER_ALL_CONTROL_ALLOW_ORIGIN_ACCESS);
                httpHeaders.setAccessControlAllowMethods(HEADER_CROSS_ACCESS_ALLOW_HTTP_METHODS);
                httpHeaders.setAccessControlMaxAge(HEADER_CROSS_ACCESS_ALLOW_MAX_AGE);
                httpHeaders.setAccessControlAllowHeaders(HEADER_CROSS_ACCESS_ALLOW_ALLOW_HEADERS);
                response.setStatusCode(HttpStatus.OK);
                return Mono.empty();
            }
        }

        return webFilterChain.filter(serverWebExchange);
    }
}
