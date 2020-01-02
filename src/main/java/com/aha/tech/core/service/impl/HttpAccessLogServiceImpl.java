package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.entity.RequestLog;
import com.aha.tech.core.service.AccessLogService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_ATTR;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/4/9
 */
@Service("httpAccessLogService")
public class HttpAccessLogServiceImpl implements AccessLogService {

    private static final Logger logger = LoggerFactory.getLogger(HttpAccessLogServiceImpl.class);

    @Resource
    private ThreadPoolTaskExecutor writeLoggingThreadPool;

    /**
     * 打印请求地址
     * @param exchange
     * @param cost
     * @return
     */
    @Override
    public String requestLog(ServerWebExchange exchange, Long cost) {
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        String traceId = ExchangeSupport.getTraceId(exchange);
        RequestLog requestLog = new RequestLog();
        URI uri = cacheRequestEntity.getRequestLine();
        if (uri == null) {
            uri = exchange.getRequest().getURI();
        }

        HttpHeaders httpHeaders = cacheRequestEntity.getAfterModifyRequestHttpHeaders();
        if (httpHeaders == null) {
            httpHeaders = exchange.getRequest().getHeaders();
        }

        Map<String, String> ipLimiterHeader = ExchangeSupport.getCurrentIpLimiter(exchange);

        requestLog.setRequestId(traceId);
        requestLog.setUri(uri);
        requestLog.setHttpHeaders(httpHeaders);
        requestLog.setBody(cacheRequestEntity.getRequestBody());
        requestLog.setCost(cost);
        requestLog.setIpLimitRemaining(ipLimiterHeader.getOrDefault("X-RateLimit-Remaining", Strings.EMPTY));
        requestLog.setIpLimitBurstCapacity(ipLimiterHeader.getOrDefault("X-RateLimit-Burst-Capacity", Strings.EMPTY));
        requestLog.setIpLimitReplenishRate(ipLimiterHeader.getOrDefault("X-RateLimit-Replenish-Rate", Strings.EMPTY));
        requestLog.setRealIp(ipLimiterHeader.getOrDefault("realIp", Strings.EMPTY));

        return requestLog.toString();
    }

    /**
     * 构建访问日志前缀
     * @param serverHttpRequest
     * @param startTime
     * @return
     */
    public String printAccessLogging(ServerHttpRequest serverHttpRequest, Long startTime, Long endTime, HttpStatus status) {
//        Long id = IdWorker.getInstance().nextId();
        String remoteIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        HttpMethod httpMethod = serverHttpRequest.getMethod();
        // cookies
        MultiValueMap<String, HttpCookie> cookieMultiValueMap = serverHttpRequest.getCookies();
        String userId = getCookieOrDefault(cookieMultiValueMap, "user_id");
        String gSsId = getCookieOrDefault(cookieMultiValueMap, "gssid");
        String gUserId = getCookieOrDefault(cookieMultiValueMap, "guserid");
        String gUniqId = getCookieOrDefault(cookieMultiValueMap, "guniqid");

        URI uri = serverHttpRequest.getURI();
        String cookieString = formatCookieStr(userId, gSsId, gUserId, gUniqId);
        String date = DateUtil.dateByDefaultFormat(new Date(startTime));
        Long cost = endTime - startTime;

        String referrer = httpHeaders.getFirst(HEADER_REFERER);
        if (StringUtils.isBlank(referrer)) {
            referrer = "-";
        }

        int httpStatus = 500;
        if (status != null) {
            httpStatus = status.value();
        }

        StringBuilder path = new StringBuilder(uri.getRawPath());
        String rawQuery = uri.getRawQuery();
        if (StringUtils.isNotBlank(rawQuery)) {
            path.append(Separator.QUESTION_MARK).append(rawQuery);
        }

        String request = String.format("%s %s", httpMethod, path);
        String userAgent = getHeaderOrDefault(httpHeaders, HEADER_USER_AGENT).split(Separator.COMMA_MARK)[0];
        String realIp = getHeaderOrDefault(httpHeaders, HEADER_X_FORWARDED_FOR);
        BigDecimal formatSeconds = new BigDecimal(cost).divide(new BigDecimal(1000L));
        String log = String.format("%s - [%s] \"%s\" %s \"%s\" \"%s\" \"%s\" \"%s\" %s",
                remoteIp, date, request, httpStatus, referrer, userAgent, realIp, cookieString, formatSeconds);

        return log;
    }


    /**
     * 打印http response相关信息
     * @param serverWebExchange
     */
    @Override
    public void printWhenError(ServerWebExchange serverWebExchange, Exception e) {
        CompletableFuture.runAsync(() -> {
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

            sb.append("请求头 : ").append(ResponseSupport.formatHttpHeaders(httpHeaders));

            sb.append(System.lineSeparator());

            sb.append("请求体 : ");
            String body = serverWebExchange.getAttributes().getOrDefault(GATEWAY_REQUEST_CACHED_ATTR, Strings.EMPTY).toString();
            sb.append(body);

            logger.error("{}", sb);
            logger.error(e.getMessage(), e);
        }, writeLoggingThreadPool);
    }

    /**
     * 获取cookies的值
     * @param cookieMultiValueMap
     * @param field
     * @return
     */
    private String getCookieOrDefault(MultiValueMap<String, HttpCookie> cookieMultiValueMap, String field) {
        HttpCookie source = cookieMultiValueMap.getFirst(field);
        if (source == null) {
            return Separator.MID_LINE_MARK;
        }

        return source.getValue();
    }

    /**
     * 获取cookies的值
     * @param httpHeaders
     * @param field
     * @return
     */
    private String getHeaderOrDefault(HttpHeaders httpHeaders, String field) {
        String source = httpHeaders.getFirst(field);
        if (StringUtils.isBlank(source)) {
            return Separator.MID_LINE_MARK;
        }

        return source;
    }

    /**
     * 格式化cookies
     * @param userId
     * @param gSsId
     * @param gUserId
     * @param gUniqId
     * @return
     */
    private String formatCookieStr(String userId, String gSsId, String gUserId, String gUniqId) {
        return String.format("user_id=%s&gssid=%s&guserid=%s&guniqid=%s", userId, gSsId, gUserId, gUniqId);
    }
}
