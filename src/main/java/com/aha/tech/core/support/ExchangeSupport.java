package com.aha.tech.core.support;

import com.aha.tech.core.model.entity.CacheRequestEntity;
import org.springframework.web.server.ServerWebExchange;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR;

/**
 * @Author: luweihong
 * @Date: 2019/5/15
 */
public class ExchangeSupport {

    public static Object get(ServerWebExchange exchange, String key, Object defaultValue) {
        return exchange.getAttributes().getOrDefault(key, defaultValue);
    }

    public static void put(ServerWebExchange exchange, String key, Object value) {
        exchange.getAttributes().put(key, value);
    }

    public static CacheRequestEntity getCacheBody(ServerWebExchange exchange) {
        CacheRequestEntity cacheRequestEntity = (CacheRequestEntity) exchange.getAttributes().get(GATEWAY_REQUEST_CACHED_REQUEST_BODY_ATTR);

        return cacheRequestEntity;
    }
}
