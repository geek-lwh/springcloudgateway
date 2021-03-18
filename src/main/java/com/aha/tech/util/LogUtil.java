package com.aha.tech.util;

import com.aha.tech.core.filters.web.AcrossFilter;
import com.aha.tech.core.support.AttributeSupport;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.Random;

import static com.aha.tech.core.constant.AttributeConstant.GATEWAY_SNAPSHOT_REQUEST_ATTR;
import static com.aha.tech.core.constant.AttributeConstant.TRACE_LOG_ID;

/**
 * @Author: luweihong
 * @Date: 2020/11/19
 */
public class LogUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

//    private static final Set<String> IGNORE_TRACE_API = Sets.newHashSet("/actuator/prometheus", "/v3/logs/create","/v3/support/signature/get");

    public static String MDC_TRACE_ID = "traceId";

    /**
     * 将traceId合并到log
     * @param exchange
     * @return
     */
    public static String combineTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getAttributeOrDefault(TRACE_LOG_ID, String.valueOf(new Random().nextInt(100000)));
        MDC.put(MDC_TRACE_ID, traceId);

        return traceId;
    }

    /**
     * 拼接错误信息
     * @param serverWebExchange
     * @param e
     * @return
     */
    public static void splicingError(ServerWebExchange serverWebExchange, Exception e) {
        StringBuffer sb = baseLogStrings(serverWebExchange);
        sb.append("错误 : ");
        String error = serverWebExchange.getAttributes().getOrDefault(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR, e.getMessage()).toString();
        sb.append(error);
        sb.append(System.lineSeparator());

        logger.error(sb.toString(), e);
    }

    /**
     * 打印链路上的日志关键信息
     * /v3/support/signature/get
     */
    public static void chainInfo(ServerWebExchange serverWebExchange, String uri) {
        if (AcrossFilter.IGNORE_TRACE_API_SET.contains(uri)) {
            return;
        }
        StringBuffer sb = baseLogStrings(serverWebExchange);
        logger.info(sb.toString());
    }

    /**
     * 基础信息
     * @param exchange
     * @return
     */
    private static StringBuffer baseLogStrings(ServerWebExchange exchange) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        StringBuffer sb = new StringBuffer();
        String url = AttributeSupport.getOriginalRequestPath(exchange, serverHttpRequest.getURI().toString());
        // 请求 行
        sb.append("请求行 : ").append(serverHttpRequest.getMethod()).append(" ").append(url);
        sb.append(System.lineSeparator());

        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        sb.append("请求头 : ");
        sb.append(System.lineSeparator());
        sb.append(HeaderUtil.formatHttpHeaders(httpHeaders));

        sb.append("请求体 : ");
        sb.append(System.lineSeparator());
        String body = exchange.getAttributes().getOrDefault(GATEWAY_SNAPSHOT_REQUEST_ATTR, Strings.EMPTY).toString();
        sb.append(body);
        sb.append(System.lineSeparator());

        return sb;
    }
}
