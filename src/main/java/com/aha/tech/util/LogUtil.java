package com.aha.tech.util;

import com.aha.tech.core.support.ExchangeSupport;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.Random;

import static com.aha.tech.core.constant.AttributeConstant.GATEWAY_REQUEST_CACHED_ATTR;
import static com.aha.tech.core.constant.AttributeConstant.TRACE_LOG_ID;

/**
 * @Author: luweihong
 * @Date: 2020/11/19
 */
public class LogUtil {

    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

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
    public static String splicingError(ServerWebExchange serverWebExchange, Exception e) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        StringBuilder sb = new StringBuilder();
        String url = ExchangeSupport.getOriginalRequestPath(serverWebExchange, serverHttpRequest.getURI().toString());

        sb.append("错误 : ");
        String error = serverWebExchange.getAttributes().getOrDefault(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR, e.getMessage()).toString();
        sb.append(error).append(System.lineSeparator());

        // 请求 行
        sb.append("请求行 : ").append(serverHttpRequest.getMethod()).append(" ").append(url);
        sb.append(System.lineSeparator());

        sb.append("请求头 : ").append(HeaderUtil.formatHttpHeaders(httpHeaders));

        sb.append(System.lineSeparator());

        sb.append("请求体 : ");
        String body = serverWebExchange.getAttributes().getOrDefault(GATEWAY_REQUEST_CACHED_ATTR, Strings.EMPTY).toString();
        sb.append(body);

        return sb.toString();
    }

}
