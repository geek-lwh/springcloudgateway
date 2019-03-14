package com.aha.tech.core.filters.global;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.aha.tech.core.constant.FilterOrderedConstant.GLOBAL_MODIFY_RESPONSE_HEADER_GATEWAY_FILTER_ORDER;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 * 修改response header 网关过滤器
 */
@Component
public class ModifyResponseHeaderGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseHeaderGatewayFilter.class);

    @Override
    public int getOrder() {
        return GLOBAL_MODIFY_RESPONSE_HEADER_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行添加response header 参数过滤器");
        ServerHttpResponse response = exchange.getResponse();
        modifyResponseHeaders(response);

        return chain.filter(exchange);
    }

    /**
     * 修改response headers的值
     * @param response
     */
    private void modifyResponseHeaders(ServerHttpResponse response) {
        HttpHeaders httpHeaders = response.getHeaders();
        crossOriginSetting(httpHeaders);
    }

    /**
     * 跨域访问设置
     * @param httpHeaders
     */
    private void crossOriginSetting(HttpHeaders httpHeaders) {
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        // 表明服务器允许客户端使用 POST,PUT,GET,DELETE 发起请求
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_METHODS, "POST,PUT,GET,DELETE");

        // 表明该响应的有效时间为 10 秒
        httpHeaders.add(HEADER_ACCESS_CONTROL_MAX_AGE, "10");

        // 表明服务器允许请求中携带字段 X-PINGOTHER 与 Content-Type x-requested-with
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, "x-requested-with,Content-Type");
    }
}
