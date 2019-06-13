package com.aha.tech.core.filters.global;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

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

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        URI realServer = (URI) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_URL_ATTR, null);
        if (realServer != null) {
            String validRequestUrl = ExchangeSupport.getRequestValidPath(exchange);
            cacheRequestEntity.setRealServer(String.format("%s%s%s%s%s", realServer.getHost(), Separator.COLON_MARK, realServer.getPort(), Separator.SLASH_MARK, validRequestUrl));
            ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);
        }
        String remoteIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        HttpHeaders newHttpHeaders = httpRequestHandlerService.modifyRequestHeaders(httpHeaders, remoteIp);
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
