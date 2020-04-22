package com.aha.tech.core.filters.normal;

import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.dianping.cat.Cat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.PRE_HANDLER_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/4/10
 *
 * 访问日志打印过滤器
 */
@Component
public class PreHandlerFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(PreHandlerFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return PRE_HANDLER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // create trace id
        String traceId = Cat.createMessageId();
        ExchangeSupport.put(exchange, TRACE_ID_ATTR, traceId);
        MDC.put("traceId", traceId);

        String rawPath = exchange.getRequest().getURI().getRawPath();
        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
        // 是否跳过授权
        Boolean isSkipAuth = httpRequestHandlerService.isSkipAuth(rawPath);

        // 是否跳过url防篡改
        Boolean isSkipUrlTamperProof = httpRequestHandlerService.isSkipUrlTamperProof(rawPath, httpHeaders);

        ExchangeSupport.put(exchange, IS_SKIP_AUTH_ATTR, isSkipAuth);
        ExchangeSupport.put(exchange, IS_SKIP_URL_TAMPER_PROOF_ATTR, isSkipUrlTamperProof);
        ExchangeSupport.put(exchange, TRACE_ID_ATTR, traceId);

        logger.info("PreHandler uri : {} isSkipAuth : {},isSkipUrlTamperProof : {}", rawPath, isSkipAuth, isSkipUrlTamperProof);
        return chain.filter(exchange);
    }

}
