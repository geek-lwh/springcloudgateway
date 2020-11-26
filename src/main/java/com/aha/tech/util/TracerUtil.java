package com.aha.tech.util;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.support.ExchangeSupport;
import com.google.common.collect.Maps;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

import static com.aha.tech.core.constant.AttributeConstant.TRACE_LOG_ID;

/**
 * @Author: luweihong
 * @Date: 2020/11/11
 */
public class TracerUtil {

    private static final Logger logger = LoggerFactory.getLogger(TracerUtil.class);

    // baggage 前缀
//    public static final String BAGGAGE_PREFIX = "uberctx-";

//    public static final String BAGGAGE_HEADER_KEY = "jaeger-baggage";


    /**
     * 上报一个error在trace中
     * @param e
     * @return
     */
    public static void logError(Exception e) {
        Span span = GlobalTracer.get().activeSpan();
        Map err = Maps.newHashMapWithExpectedSize(6);
        err.put(Fields.EVENT, Tags.ERROR.getKey());
        err.put(Fields.ERROR_OBJECT, e);
        err.put(Fields.MESSAGE, e.getMessage());
        Tags.ERROR.set(span, true);
        span.log(err);
        logger.error(e.getMessage(), e);
    }

    /**
     * 上报一个error在trace中
     * @param e
     * @return
     */
    public static void logError(Exception e, Span span) {
        Map err = Maps.newHashMapWithExpectedSize(6);
        err.put(Fields.EVENT, Tags.ERROR.getKey());
        err.put(Fields.ERROR_OBJECT, e);
        err.put(Fields.MESSAGE, e.getMessage());
        Tags.ERROR.set(span, true);
        span.log(err);
        logger.error(e.getMessage(), e);
    }


    /**
     * 设置每个tace中span的线索
     * @param span
     */
    public static void setClue(Span span, ServerWebExchange exchange) {
        span.setTag(HeaderFieldConstant.TRACE_ID, span.context().toTraceId());
        span.setTag(HeaderFieldConstant.SPAN_ID, span.context().toSpanId());
        ExchangeSupport.put(exchange, TRACE_LOG_ID, span.context().toTraceId());
        ExchangeSupport.setActiveSpan(exchange, span);
    }

    /**
     *
     * 设置span的父子关系并且开启一个span
     * @param exchange
     * @param operationName
     * @return
     */
    public static Span startAndRef(ServerWebExchange exchange, String operationName) {
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        Span parentSpan = ExchangeSupport.getActiveSpan(exchange);
        return spanBuilder.asChildOf(parentSpan).start();
    }
}
