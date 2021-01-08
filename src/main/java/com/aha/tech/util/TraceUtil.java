package com.aha.tech.util;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.support.AttributeSupport;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2020/11/11
 */
public class TraceUtil {

    private static final Logger logger = LoggerFactory.getLogger(TraceUtil.class);


    /**
     * 设置每个tace中span的线索
     * @param span
     */
    public static void setActiveSpan(Span span, ServerWebExchange exchange) {
        span.setTag(HeaderFieldConstant.TRACE_ID, span.context().toTraceId());
        span.setTag(HeaderFieldConstant.SPAN_ID, span.context().toSpanId());
        AttributeSupport.setActiveSpan(exchange, span);
    }

    /**
     *
     * 设置span的父子关系并且开启一个span
     * @param exchange
     * @param operationName
     * @return
     */
    public static Span start(ServerWebExchange exchange, String operationName) {
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        Span parentSpan = AttributeSupport.getActiveSpan(exchange);
        Span span = spanBuilder.asChildOf(parentSpan).start();
        TraceUtil.setActiveSpan(span, exchange);
        LogUtil.combineTraceId(exchange);

        return span;
    }


}
