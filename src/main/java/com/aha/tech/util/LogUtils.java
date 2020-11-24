package com.aha.tech.util;

import org.slf4j.MDC;
import org.springframework.web.server.ServerWebExchange;

import java.util.Random;

import static com.aha.tech.core.constant.AttributeConstant.TRACE_LOG_ID;

/**
 * @Author: luweihong
 * @Date: 2020/11/19
 */
public class LogUtils {

    public static String MDC_TRACE_ID = "traceId";

    public static String combineTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getAttributeOrDefault(TRACE_LOG_ID, String.valueOf(new Random().nextInt(100000)));
        MDC.put(MDC_TRACE_ID, traceId);

        return traceId;
    }
}
