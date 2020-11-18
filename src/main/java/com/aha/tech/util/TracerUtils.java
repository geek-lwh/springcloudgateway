package com.aha.tech.util;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.google.common.collect.Maps;
import io.opentracing.Span;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Author: luweihong
 * @Date: 2020/11/11
 */
public class TracerUtils {

    private static final Logger logger = LoggerFactory.getLogger(TracerUtils.class);

    public static final String CLASS = "class";

    public static final String METHOD = "method";

    public static final String SQL = "sql";

    // baggage 前缀
    public static final String BAGGAGE_PREFIX = "uberctx-";

    public static final String BAGGAGE_HEADER_KEY = "jaeger-baggage";


    /**
     * 上报一个error在trace中
     * @param e
     * @return
     */
    public static void reportErrorTrace(Exception e) {
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
     * 设置每个tace中span的线索
     * @param span
     */
    public static void setClue(Span span) {
        span.setTag(HeaderFieldConstant.TRACE_ID, span.context().toTraceId());
        span.setTag(HeaderFieldConstant.SPAN_ID, span.context().toSpanId());
    }

}
