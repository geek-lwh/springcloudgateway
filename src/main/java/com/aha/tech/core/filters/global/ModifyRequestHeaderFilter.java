package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.wrapper.ServletRequestCarrierWrapper;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.util.LogUtils;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
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

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER;

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

    @Resource
    private Tracer tracer;

    @Override
    public int getOrder() {
        return MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = TracerUtils.startAndRef(exchange, this.getClass().getSimpleName());
        LogUtils.combineLog(exchange);
        ExchangeSupport.setSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span, exchange);
            ServerHttpRequest newRequest = replaceHeader(exchange, span);

            return chain.filter(exchange.mutate().request(newRequest).build());
        } catch (Exception e) {
            TracerUtils.logError(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    /**
     * 重构header
     * @param exchange
     * @return
     */
    private ServerHttpRequest replaceHeader(ServerWebExchange exchange, Span span) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpHeaders originalHeaders = serverHttpRequest.getHeaders();
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        String remoteIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        HttpHeaders newHttpHeaders = httpRequestHandlerService.modifyRequestHeaders(exchange, originalHeaders, remoteIp);

        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new ServletRequestCarrierWrapper(newHttpHeaders));

        cacheRequestEntity.setOriginalRequestHttpHeaders(originalHeaders);
        cacheRequestEntity.setAfterModifyRequestHttpHeaders(newHttpHeaders);

        ServerHttpRequest newRequest = new ServerHttpRequestDecorator(serverHttpRequest) {
            @Override
            public HttpHeaders getHeaders() {
                return newHttpHeaders;
            }
        };

        return newRequest;
    }

}
