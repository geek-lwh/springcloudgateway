package com.aha.tech.core.support;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.google.common.collect.Maps;
import io.opentracing.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static com.aha.tech.core.constant.AttributeConstant.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/5/15
 *
 * 获取exchange attr 帮助类
 */
public class ExchangeSupport {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeSupport.class);

    /**
     * 获取key value default
     * @param exchange
     * @param key
     * @param defaultValue
     * @return
     */
    public static Object get(ServerWebExchange exchange, String key, Object defaultValue) {
        return exchange.getAttributes().getOrDefault(key, defaultValue);
    }

    /**
     * 设置 key value
     * @param exchange
     * @param key
     * @param value
     */
    public static void put(ServerWebExchange exchange, String key, Object value) {
        exchange.getAttributes().put(key, value);
    }

    /**
     * 设置responseBody
     * @param exchange
     * @param responseBody
     */
    public static void putResponseBody(ServerWebExchange exchange, String responseBody) {
        exchange.getAttributes().put(RESPONSE_BODY, responseBody);
    }

    /**
     * 设置responseBody
     * @param exchange
     */
    public static String getResponseBody(ServerWebExchange exchange) {
        return exchange.getAttributes().getOrDefault(RESPONSE_BODY, Strings.EMPTY).toString();
    }

    /**
     * 获取请求的原始路径
     * @param exchange
     * @param defaultPath
     * @return
     */
    public static String getOriginalRequestPath(ServerWebExchange exchange, String defaultPath) {
        return (String) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, defaultPath);
    }

    /**
     * 获取路由的路径
     * @param exchange
     * @return
     */
    public static String getRouteRequestPath(ServerWebExchange exchange) {
        URI realServer = (URI) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_URL_ATTR, null);
        String routeHost;
        if (realServer == null) {
            routeHost = exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_ROUTE_HOST_ATTR, StringUtils.EMPTY).toString();
        } else {
            routeHost = String.format("%s:%s", realServer.getHost(), realServer.getPort());
        }

        return routeHost;
    }


    /**
     * 获取请求缓存
     * @param exchange
     * @return
     */
    public static CacheRequestEntity getCacheRequest(ServerWebExchange exchange) {
        CacheRequestEntity cacheRequestEntity = (CacheRequestEntity) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_CACHED_ATTR, null);
        if (cacheRequestEntity == null) {
            cacheRequestEntity = new CacheRequestEntity();
            exchange.getAttributes().put(GATEWAY_REQUEST_CACHED_ATTR, cacheRequestEntity);
        }
        return cacheRequestEntity;
    }

    /**
     * 获取是否跳过授权信息
     * @param exchange
     * @return
     */
    public static Boolean getIsSkipAuth(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_SKIP_AUTH_ATTR, Boolean.FALSE);
    }

    /**
     * 获取是否跳过url防篡改
     * @param exchange
     * @return
     */
    public static Boolean getIsSkipUrlTamperProof(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_SKIP_URL_TAMPER_PROOF_ATTR, Boolean.FALSE);
    }

    /**
     * 获取拦截添加的请求信息
     * @param exchange
     * @return
     */
    public static RequestAddParamsDto getRequestAddParamsDto(ServerWebExchange exchange) {
        RequestAddParamsDto requestAddParamsDto = (RequestAddParamsDto) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_ADD_PARAMS_ATTR, null);
        if (requestAddParamsDto == null) {
            logger.warn("需要添加的参数为空");
            requestAddParamsDto = new RequestAddParamsDto();
            requestAddParamsDto.setUserId(null);
        }

        return requestAddParamsDto;
    }


    /**
     *
     * @param exchange
     * @param response
     * @param realIp
     */
    public static void setIpLimiterCache(ServerWebExchange exchange, RateLimiter.Response response, String realIp) {
        Map<String, String> ipLimiterMap = Maps.newHashMap();
        ipLimiterMap.putAll(response.getHeaders());
        ipLimiterMap.put("realIp", realIp);
        exchange.getAttributes().put(IP_LIMITER_ATTR, ipLimiterMap);
    }

    public static Map<String, String> getCurrentIpLimiter(ServerWebExchange exchange) {
        return (Map<String, String>) exchange.getAttributes().getOrDefault(IP_LIMITER_ATTR, Collections.EMPTY_MAP);
    }

    /**
     * 是否是老版本
     * @param exchange
     * @return
     */
    public static Boolean isOldVersion(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_OLD_VERSION_ATTR, Boolean.FALSE);
    }

    /**
     * 设置当前span
     * @param exchange
     * @param span
     */
    public static void setActiveSpan(ServerWebExchange exchange, Span span) {
        exchange.getAttributes().put(ACTIVE_SPAN, span);
    }

    /**
     * 获取当前span
     * @param exchange
     * @return
     */
    public static Span getActiveSpan(ServerWebExchange exchange) {
        return (Span) exchange.getAttributes().getOrDefault(ACTIVE_SPAN, null);
    }

    /**
     * 设置http status状态
     * @param exchange
     * @param httpStatus
     */
    public static void setHttpStatus(ServerWebExchange exchange, HttpStatus httpStatus) {
        exchange.getAttributes().put(HTTP_STATUS, httpStatus.value());
    }

    /**
     * 获取http status 状态
     * @param exchange
     * @return
     */
    public static int getHttpStatus(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(HTTP_STATUS, HttpStatus.OK.value());
    }

    /**
     * 设置父类span
     * @param exchange
     * @param span
     */
    public static void setParentSpan(ServerWebExchange exchange, Span span) {
        exchange.getAttributes().put(PARENT_SPAN, span);
    }

    /**
     * 获取父类span
     * @param exchange
     * @return
     */
    public static Span getParentSpan(ServerWebExchange exchange) {
        Object span = exchange.getAttributes().get(PARENT_SPAN);
        if (span == null) {
            span = getActiveSpan(exchange);
            if (span == null) {
                return null;
            }
        }

        return (Span) span;
    }

}
