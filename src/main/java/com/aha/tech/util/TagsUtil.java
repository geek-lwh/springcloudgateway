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
 * @Date: 2020/12/9
 */
public class TagsUtil {

    private static final Logger logger = LoggerFactory.getLogger(TagsUtil.class);

    public static final String ERROR = "error";

    // baggage 前缀
//    public static final String BAGGAGE_PREFIX = "uberctx-";

//    public static final String BAGGAGE_HEADER_KEY = "jaeger-baggage";


    /**
     * 上报一个error在trace中
     * @param e
     * @return
     */
    public static void setCapturedErrorsTags(Exception e) {
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
    public static void setCapturedErrorsTags(Exception e, Span span) {
        Map err = Maps.newHashMapWithExpectedSize(6);
        err.put(Fields.EVENT, ERROR);
        err.put(Fields.MESSAGE, e.getMessage());
        err.put(Fields.ERROR_OBJECT, e);
        err.put(Fields.STACK, e.getStackTrace()[0]);
        Tags.ERROR.set(span, true);
        span.log(err);
        logger.error(e.getMessage(), e);
    }

    /**
     * 设置rpcClient的tag信息
     * @param span
     * @param type
     */
    public static void setRpcTags(Span span, String type) {
        Tags.SPAN_KIND.set(span, type);
        setChain(span);
    }

    /**
     * 设置链路信息
     * @param span
     */
    public static void setChain(Span span) {
        span.setTag(HeaderFieldConstant.TRACE_ID, span.context().toTraceId());
        span.setTag(HeaderFieldConstant.SPAN_ID, span.context().toSpanId());
    }

}
