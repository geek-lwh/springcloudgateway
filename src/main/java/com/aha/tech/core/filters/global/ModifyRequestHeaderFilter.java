package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
import static com.aha.tech.core.constant.HeaderFieldConstant.REQUEST_ID;
import static com.aha.tech.core.interceptor.FeignRequestInterceptor.TRACE_ID;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 *
 * 修改请求头网关过滤器
 */
@Component
public class ModifyRequestHeaderFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestHeaderFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始进行修改请求头网关过滤器");
        List<String> clientRequestId = exchange.getRequest().getHeaders().get(REQUEST_ID);
        if (!CollectionUtils.isEmpty(clientRequestId)) {
            MDC.put(TRACE_ID, clientRequestId.get(0));
        }
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpHeaders originalHeaders = serverHttpRequest.getHeaders();
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        String remoteIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        HttpHeaders newHttpHeaders = httpRequestHandlerService.modifyRequestHeaders(exchange, originalHeaders, remoteIp);
        logger.debug("after modify request header : {} ", ResponseSupport.formatHttpHeaders(newHttpHeaders));
        cacheRequestEntity.setOriginalRequestHttpHeaders(originalHeaders);
        cacheRequestEntity.setAfterModifyRequestHttpHeaders(newHttpHeaders);

        ServerHttpRequest newRequest = new ServerHttpRequestDecorator(serverHttpRequest) {
            @Override
            public HttpHeaders getHeaders() {
                return newHttpHeaders;
            }
        };

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

}
