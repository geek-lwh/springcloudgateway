package com.aha.tech.core.filters.global;

import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
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

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_RESPONSE_GATEWAY_FILTER_ORDER;

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
    private Tracer tracer;

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = TracerUtils.startAndRef(exchange, this.getClass().getSimpleName());
        ExchangeSupport.setSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span, exchange);
            ServerHttpResponseDecorator serverHttpResponseDecorator = mutateResponse(exchange, chain);

            return chain.filter(exchange.mutate().response(serverHttpResponseDecorator).build());
        } catch (Exception e) {
            TracerUtils.logError(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    /**
     * 转变response对象
     * @param exchange
     * @param chain
     * @return
     */
    private ServerHttpResponseDecorator mutateResponse(ServerWebExchange exchange, GatewayFilterChain chain) {
        // todo 异步
        ServerHttpResponseDecorator serverHttpResponseDecorator = httpRequestHandlerService.modifyResponseBodyAndHeaders(exchange);

        return serverHttpResponseDecorator;
    }

}
