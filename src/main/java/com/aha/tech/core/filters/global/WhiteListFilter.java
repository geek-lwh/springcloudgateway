package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.util.IdWorker;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.WHITE_LIST_REQUEST_FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/5/8
 */
@Component
public class WhiteListFilter implements GlobalFilter, Ordered {

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return WHITE_LIST_REQUEST_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
        // 是否跳过授权
        Boolean isSkipAuth = httpRequestHandlerService.isSkipAuth(rawPath);

        // 是否跳过url防篡改
        Boolean isSkipUrlTamperProof = httpRequestHandlerService.isSkipUrlTamperProof(rawPath, httpHeaders);

        // create trace id
        String traceId = String.valueOf(IdWorker.getInstance().nextId());
        ExchangeSupport.put(exchange, TRACE_ID_ATTR, traceId);

        ExchangeSupport.put(exchange, IS_SKIP_AUTH_ATTR, isSkipAuth);
        ExchangeSupport.put(exchange, IS_SKIP_URL_TAMPER_PROOF_ATTR, isSkipUrlTamperProof);

        return chain.filter(exchange);
    }

}
