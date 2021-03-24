package com.aha.tech.core.support;

import com.aha.tech.core.model.dto.BaggageItemDto;
import com.aha.tech.core.model.entity.SnapshotRequestEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.google.common.collect.Maps;
import io.opentracing.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collections;
import java.util.Map;

import static com.aha.tech.core.constant.AttributeConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/5/15
 *
 * 获取exchange attr 帮助类
 */
public class AttributeSupport {

    private static final Logger logger = LoggerFactory.getLogger(AttributeSupport.class);

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
     * 在exchange中设置key value
     * @param exchange
     * @param key
     * @param value
     */
    public static void put(ServerWebExchange exchange, String key, Object value) {
        exchange.getAttributes().put(key, value);
    }

    /**
     * 在exchange设置的同时,在span中也设置
     * @param exchange
     * @param span
     * @param key
     * @param value
     */
    public static void put(ServerWebExchange exchange, Span span, String key, Object value) {
        exchange.getAttributes().put(key, value);
        span.setTag(key, String.valueOf(value));
    }

    /**
     * 在exchange中设置错误信息
     * @param exchange
     * @param errorMsg
     */
    public static void fillErrorMsg(ServerWebExchange exchange, String errorMsg) {
        exchange.getAttributes().put(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR, errorMsg);
    }

    /**
     * 设置responseBody
     * @param exchange
     * @param responseVo
     */
    public static void putResponseLine(ServerWebExchange exchange, ResponseVo responseVo) {
        exchange.getAttributes().put(RESPONSE_LINE, responseVo);
    }

    /**
     * 设置responseBody
     * @param exchange
     */
    public static ResponseVo getResponseLine(ServerWebExchange exchange) {
        return (ResponseVo) exchange.getAttributes().getOrDefault(RESPONSE_LINE, new ResponseVo());
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
     * 获取请求缓存
     * @param exchange
     * @return
     */
    public static SnapshotRequestEntity getSnapshotRequest(ServerWebExchange exchange) {
        Object snapshotRequestEntity = exchange.getAttributes().get(GATEWAY_SNAPSHOT_REQUEST_ATTR);
        if (snapshotRequestEntity == null) {
            snapshotRequestEntity = new SnapshotRequestEntity();
            exchange.getAttributes().put(GATEWAY_SNAPSHOT_REQUEST_ATTR, snapshotRequestEntity);
        }

        return (SnapshotRequestEntity) snapshotRequestEntity;
    }

    /**
     * 获取是否跳过授权信息
     * @param exchange
     * @return
     */
    public static Boolean isSkipAuth(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_SKIP_AUTH_ATTR, Boolean.FALSE);
    }

    /**
     * 获取是否忽略授权信息
     * @param exchange
     * @return
     */
    public static Boolean ignoreEmptyKidMapping(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_IGNORE_EMPTY_KID_MAPPING_ATTR, Boolean.FALSE);
    }

    /**
     * 获取是否跳过ip限流
     * @param exchange
     * @return
     */
    public static Boolean getIsSkipIpLimiter(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_SKIP_IP_LIMITER_ATTR, Boolean.FALSE);
    }

    /**
     * 获取是否跳过ip限流
     * @param exchange
     * @return
     */
    public static String getRequestIp(ServerWebExchange exchange) {
        return (String) exchange.getAttributes().getOrDefault(REQUEST_IP_ATTR, exchange.getRequest().getRemoteAddress().getHostString());
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
    public static BaggageItemDto getRequestAddParamsDto(ServerWebExchange exchange) {
        BaggageItemDto requestAddParamsDto = (BaggageItemDto) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_ADD_PARAMS_ATTR, null);
        if (requestAddParamsDto == null) {
            logger.warn("需要添加的参数为空");
            requestAddParamsDto = new BaggageItemDto();
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
    public static int responseStatus(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(HTTP_STATUS, HttpStatus.OK.value());
    }

    /**
     * 是否需要强制升级
     * @param exchange
     * @return
     */
    public static Boolean shouldClientUpgrade(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_NEED_UPGRADE_ATTR, Boolean.FALSE);
    }

}
