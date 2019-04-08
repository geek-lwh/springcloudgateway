package com.aha.tech.core.filters.global;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.ACCESS_LOG_FILTER_ORDER;
import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_X_FORWARDED_FOR;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 */
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    public int getOrder() {
        return ACCESS_LOG_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();

        Map<String, Object> attributes = exchange.getAttributes();
        String id = serverHttpRequest.getId();
        attributes.put(ACCESS_REQUEST_ID_ATTR, id);

        Long requestTime = System.currentTimeMillis();
        attributes.put(ACCESS_REQUEST_TIME_ATTR, requestTime);

        String remoteIp = serverHttpRequest.getRemoteAddress().getAddress().getHostAddress();
        attributes.put(ACCESS_REMOTE_IP_ATTR, remoteIp);

        List<String> forwardedIps = serverHttpRequest.getHeaders().get(HEADER_X_FORWARDED_FOR);
        String forwardIp = CollectionUtils.isEmpty(forwardedIps) ? Strings.EMPTY : forwardedIps.get(0);
        attributes.put(ACCESS_X_FORWARDED_IP_ATTR, forwardIp);

        // cookies
        MultiValueMap<String, HttpCookie> cookieMultiValueMap = serverHttpRequest.getCookies();
        List<String> userIdList = cookieMultiValueMap.getOrDefault("user_id", Collections.EMPTY_LIST);
        List<String> gSsIdList = cookieMultiValueMap.getOrDefault("gssid", Collections.EMPTY_LIST);
        List<String> gUserIdList = cookieMultiValueMap.getOrDefault("guserid", Collections.EMPTY_LIST);
        List<String> gUniqIdList = cookieMultiValueMap.getOrDefault("guniqid", Collections.EMPTY_LIST);

        String userId = CollectionUtils.isEmpty(userIdList) ? Strings.EMPTY : userIdList.get(0);
        String gSsId = CollectionUtils.isEmpty(gSsIdList) ? Strings.EMPTY : gSsIdList.get(0);
        String gUserId = CollectionUtils.isEmpty(gUserIdList) ? Strings.EMPTY : gUserIdList.get(0);
        String gUniqId = CollectionUtils.isEmpty(gUniqIdList) ? Strings.EMPTY : gUniqIdList.get(0);

        String cookieStr = String.format("user_id=%s-&gssid=%s-&guserid=%s-&guniqid=%s-", userId, gSsId, gUserId, gUniqId);
        attributes.put(ACCESS_LOG_COOKIE_ATTR, cookieStr);

        return chain.filter(exchange);
    }


}
