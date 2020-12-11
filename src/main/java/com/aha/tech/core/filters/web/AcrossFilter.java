package com.aha.tech.core.filters.web;

import com.aha.tech.core.constant.AttributeConstant;
import com.aha.tech.core.support.AttributeSupport;
import com.aha.tech.util.LogUtil;
import com.aha.tech.util.TraceUtil;
import com.google.common.collect.Sets;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

import static com.aha.tech.core.constant.AttributeConstant.HTTP_STATUS;
import static com.aha.tech.core.constant.AttributeConstant.TRACE_LOG_ID;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/4/10
 * 贯穿整个网关filter生命周期的filter
 * 最先执行的filter
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcrossFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(AcrossFilter.class);

    private static final Set<String> IGNORE_TRACE_API_SET = Sets.newHashSet("/actuator/prometheus", "/v3/logs/create");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain webFilterChain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders respHeaders = response.getHeaders();
        respHeaders.setAccessControlAllowOrigin(HEADER_ALL_CONTROL_ALLOW_ORIGIN_ACCESS);
        respHeaders.setAccessControlAllowMethods(HEADER_CROSS_ACCESS_ALLOW_HTTP_METHODS);
        respHeaders.setAccessControlMaxAge(HEADER_CROSS_ACCESS_ALLOW_MAX_AGE);
        respHeaders.setAccessControlAllowHeaders(HEADER_CROSS_ACCESS_ALLOW_ALLOW_HEADERS);
        if (request.getMethod() == HttpMethod.OPTIONS) {
            response.setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }

        String uri = exchange.getRequest().getURI().getRawPath();

        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(uri);
        Span span = spanBuilder.start();
        String traceId = span.context().toTraceId();
        if (IGNORE_TRACE_API_SET.contains(uri)) {
            Tags.SAMPLING_PRIORITY.set(span, 0);
        }

        try (Scope scope = tracer.scopeManager().activate(span)) {
            TraceUtil.setActiveSpan(span, exchange);
            AttributeSupport.put(exchange, TRACE_LOG_ID, traceId);
            respHeaders.set(AttributeConstant.TRACE_LOG_ID, traceId);
            return webFilterChain.filter(exchange).doFinally((s) -> {
                LogUtil.chainInfo(exchange, uri);
                int status = AttributeSupport.getHttpStatus(exchange);
                span.setTag(HTTP_STATUS, status);
                String error = exchange.getAttributes().getOrDefault(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR, Strings.EMPTY).toString();
                if (StringUtils.isNotBlank(error) || status > HttpStatus.OK.value()) {
                    span.log(error);
                    Tags.ERROR.set(span, true);
                }

                span.finish();
            });
        }

    }
}
