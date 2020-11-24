package com.aha.tech.util;

import org.slf4j.MDC;
import org.springframework.web.server.ServerWebExchange;

import static com.aha.tech.core.constant.AttributeConstant.TRACE_LOG_ID;

/**
 * @Author: luweihong
 * @Date: 2020/11/19
 */
public class LogUtils {

    public static String combineLog(ServerWebExchange exchange) {
        String traceId = exchange.getAttributeOrDefault(TRACE_LOG_ID, "MISS_TRACE_ID");
        MDC.put("traceId", traceId);
        return traceId;
    }
}
