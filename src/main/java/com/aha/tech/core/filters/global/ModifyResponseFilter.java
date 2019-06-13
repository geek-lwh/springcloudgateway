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
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_ATTR;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_RESPONSE_GATEWAY_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/4/1
 */
@Component
public class ModifyResponseFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyResponseFilter.class);

    @Override
    public int getOrder() {
        return MODIFY_RESPONSE_GATEWAY_FILTER_ORDER;
    }

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        URI realServer = (URI) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_URL_ATTR, null);
        if (realServer != null) {
            String validRequestUrl = ExchangeSupport.getRequestValidPath(exchange);
            String realServerUrl = String.format("%s%s%s%s%s", realServer.getHost(), Separator.COLON_MARK, realServer.getPort(), Separator.SLASH_MARK, validRequestUrl);
            cacheRequestEntity.setRealServer(realServerUrl);
            ExchangeSupport.put(exchange, GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);
        }
        ServerHttpResponseDecorator serverHttpResponseDecorator = httpRequestHandlerService.modifyResponseBodyAndHeaders(exchange);
        return chain.filter(exchange.mutate().response(serverHttpResponseDecorator).build());
    }

}
