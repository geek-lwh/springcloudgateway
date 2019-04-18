package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.service.AccessLogService;
import com.aha.tech.util.IdWorker;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR;
import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_USER_AGENT;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_X_FORWARDED_FOR;

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
     * 构建访问日志前缀
     * @param serverHttpRequest
     * @param startTime
     * @return
     */
    public String printAccessLogging(ServerHttpRequest serverHttpRequest, Long startTime, Long endTime, HttpStatus status) {
        Long id = IdWorker.getInstance().nextId();
        String remoteIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        List<String> forwardedIps = serverHttpRequest.getHeaders().get(HEADER_X_FORWARDED_FOR);
        List<String> userAgent = serverHttpRequest.getHeaders().get(HEADER_USER_AGENT);

        // cookies
        MultiValueMap<String, HttpCookie> cookieMultiValueMap = serverHttpRequest.getCookies();
        String userId = getValueOrDefault(cookieMultiValueMap, "user_id");
        String gSsId = getValueOrDefault(cookieMultiValueMap, "gssid");
        String gUserId = getValueOrDefault(cookieMultiValueMap, "guserid");
        String gUniqId = getValueOrDefault(cookieMultiValueMap, "guniqid");

        URI uri = serverHttpRequest.getURI();
        String cookieString = formatCookieStr(userId, gSsId, gUserId, gUniqId);
        String date = DateUtil.dateByDefaultFormat(new Date(startTime));
        Long cost = endTime - startTime;
        String log = String.format("[request_id=%s,time=%s,uri=%s,remote_ip=%s,forwarded_ip=%s,cookie_string=%s,user_agent=%s,status=%s,cost=%sms]",
                id, date, uri, remoteIp, forwardedIps, cookieString, userAgent, status, cost);

        return log;
    }

    /**
     * 打印http response相关信息
     * @param serverWebExchange
     */
    @Override
    public void printWhenError(ServerWebExchange serverWebExchange, String errorMsg) {
        ServerHttpRequest serverHttpRequest = serverWebExchange.getRequest();
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        CompletableFuture.runAsync(() -> {
            StringBuilder sb = new StringBuilder();
            String url = serverWebExchange.getAttributes().getOrDefault(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, Strings.EMPTY).toString();

            sb.append("错误 : ");
            String error = serverWebExchange.getAttributes().getOrDefault(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR, errorMsg).toString();
            sb.append(error).append(System.lineSeparator());

            // 请求 行
            sb.append("请求行 : ").append(serverHttpRequest.getMethod()).append(" ").append(url);
            sb.append(System.lineSeparator());

            sb.append("请求头 : ");
            httpHeaders.forEach((key, value) -> sb.append(key).append(Separator.EQUAL_SIGN_MARK).append(value).append(" "));
            sb.append(System.lineSeparator());

            sb.append("请求体 : ");
            String body = serverWebExchange.getAttributes().getOrDefault(GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR, Strings.EMPTY).toString();
            sb.append(body);



            logger.error("{}", sb);
        }, writeLoggingThreadPool);
    }

    /**
     * 获取cookies的值
     * @param cookieMultiValueMap
     * @param field
     * @return
     */
    private String getValueOrDefault(MultiValueMap<String, HttpCookie> cookieMultiValueMap, String field) {
        HttpCookie source = cookieMultiValueMap.getFirst(field);
        if (source == null) {
            return Strings.EMPTY;
        }

        return source.getValue();
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
        return String.format("|user_id=%s|&gssid=%s|&guserid=%s|&guniqid=%s|", userId, gSsId, gUserId, gUniqId);
    }
}
