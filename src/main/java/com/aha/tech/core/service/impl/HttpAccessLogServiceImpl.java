package com.aha.tech.core.service.impl;

import com.aha.tech.core.service.AccessLogService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.ACCESS_REQUEST_ID_ATTR;
import static com.aha.tech.core.constant.ExchangeAttributeConstant.ACCESS_REQUEST_TIME_ATTR;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_X_FORWARDED_FOR;

/**
 * @Author: luweihong
 * @Date: 2019/4/9
 */
@Service("httpAccessLogService")
public class HttpAccessLogServiceImpl implements AccessLogService {

    private static final Logger logger = LoggerFactory.getLogger(HttpAccessLogServiceImpl.class);

    /**
     * 打印http请求信息
     * @param serverHttpRequest
     * @param id
     * @param requestTime
     */
    @Override
    public void printRequestInfo(ServerHttpRequest serverHttpRequest, String id, Long requestTime) {
        String remoteIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        List<String> forwardedIps = serverHttpRequest.getHeaders().get(HEADER_X_FORWARDED_FOR);

        // cookies
        MultiValueMap<String, HttpCookie> cookieMultiValueMap = serverHttpRequest.getCookies();
        String userId = getValueOrDefault(cookieMultiValueMap, "user_id");
        String gSsId = getValueOrDefault(cookieMultiValueMap, "gssid");
        String gUserId = getValueOrDefault(cookieMultiValueMap, "guserid");
        String gUniqId = getValueOrDefault(cookieMultiValueMap, "guniqid");

        String cookieString = formatCookieStr(userId, gSsId, gUserId, gUniqId);

        String log = String.format("id=%s,time=%s,remote_ip=%s,forwarded_ip=%s,cookie_string=%s", id, new Date(requestTime), remoteIp, forwardedIps, cookieString);

        logger.info("access_log : {}", log);
    }

    /**
     * 打印http response相关信息
     * @param serverHttpResponse
     * @param attributes
     */
    @Override
    public void printResponseInfo(ServerHttpResponse serverHttpResponse, Map<String, Object> attributes) {
        String id = attributes.getOrDefault(ACCESS_REQUEST_ID_ATTR, Strings.EMPTY).toString();
        Long requestTime = (Long) attributes.getOrDefault(ACCESS_REQUEST_TIME_ATTR, System.currentTimeMillis());
        Long endTime = System.currentTimeMillis();
        Long cost = endTime - requestTime;

        logger.info("id={},status={},cost={}", id, serverHttpResponse.getStatusCode(), cost);
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
        return String.format("user_id=%s-&gssid=%s-&guserid=%s-&guniqid=%s-", userId, gSsId, gUserId, gUniqId);
    }
}
