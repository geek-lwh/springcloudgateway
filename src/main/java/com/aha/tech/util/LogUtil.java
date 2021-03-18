package com.aha.tech.util;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.core.filters.web.AcrossFilter;
import com.aha.tech.core.model.entity.SnapshotRequestEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.support.AttributeSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.Random;

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
        StringBuffer sb = appendingLog(serverWebExchange);
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
    public static void output(ServerWebExchange serverWebExchange, String uri, Boolean isError, String errMsg) {
        if (AcrossFilter.IGNORE_TRACE_API_SET.contains(uri) && !isError) {
            return;
        }

        ResponseVo responseVo = AttributeSupport.getResponseLine(serverWebExchange);
        int code = responseVo.getCode();
        Object responseData = responseVo.getData();
        SnapshotRequestEntity snapshotRequestEntity = AttributeSupport.getSnapshotRequest(serverWebExchange);
        StringBuffer sb = appendingLog(snapshotRequestEntity, responseData);
        logByLevel(sb.toString(), code, isError, errMsg);
    }

    /**
     * 拼接日志
     * @param snapshotRequestEntity
     * @param responseData
     * @return
     */
    private static StringBuffer appendingLog(SnapshotRequestEntity snapshotRequestEntity, Object responseData) {

        StringBuffer sb = new StringBuffer();

        sb.append("<=================================================").append(System.lineSeparator());
        // 请求 行
        sb.append("请求行 : ").append(snapshotRequestEntity.getRequestLine());
        sb.append(System.lineSeparator());

        sb.append("原始请求头 : ");
        sb.append(System.lineSeparator());
        sb.append(HeaderUtil.formatHttpHeaders(snapshotRequestEntity.getOriginalRequestHttpHeaders()));

        sb.append("修改后请求头 : ");
        sb.append(System.lineSeparator());
        sb.append(HeaderUtil.formatHttpHeaders(snapshotRequestEntity.getAfterModifyRequestHttpHeaders()));

        sb.append("请求体 : ");
        sb.append(System.lineSeparator());
        sb.append(snapshotRequestEntity.getRequestBody());
        sb.append(System.lineSeparator());


        sb.append("响应体 : ");
        sb.append(System.lineSeparator());
        sb.append(responseData);
        sb.append(System.lineSeparator());

        sb.append("=================================================>").append(System.lineSeparator());

        return sb;
    }

    /**
     * 拼接日志
     * @param serverWebExchange
     * @return
     */
    private static StringBuffer appendingLog(ServerWebExchange serverWebExchange) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        SnapshotRequestEntity snapshotRequestEntity = AttributeSupport.getSnapshotRequest(serverWebExchange);

        StringBuffer sb = new StringBuffer();
        sb.append("<=================================================").append(System.lineSeparator());
        // 请求 行
        sb.append("请求行 : ").append(serverHttpRequest.getMethod()).append(" ").append(snapshotRequestEntity.getRequestLine());
        sb.append(System.lineSeparator());

        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        sb.append("请求头 : ");
        sb.append(System.lineSeparator());
        sb.append(HeaderUtil.formatHttpHeaders(snapshotRequestEntity.getOriginalRequestHttpHeaders()));

        sb.append("请求体 : ");
        sb.append(System.lineSeparator());
        sb.append(snapshotRequestEntity.getRequestBody());
        sb.append(System.lineSeparator());

        sb.append("=================================================>").append(System.lineSeparator());

        return sb;
    }

    /**
     * 分级输出
     * @param log
     * @param code
     * @param isError
     */
    private static void logByLevel(String log, int code, Boolean isError, String errMsg) {
        if (isError) {
            logger.error("请求出现异常 : {}", errMsg);
            logger.error(log);
        } else if (code > ResponseConstants.SUCCESS) {
            logger.warn("状态码异常: code = {}", code);
            logger.warn(log);
        } else {
            logger.info(log);
        }
    }
}
