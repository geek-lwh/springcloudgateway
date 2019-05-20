package com.aha.tech.core.support;

import com.aha.tech.core.constant.LanguageConstant;
import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/5/15
 *
 * 获取exchange attr 帮助类
 */
public class ExchangeSupport {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeSupport.class);

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

    public static String getRequestLanguage(ServerWebExchange exchange) {
        String language = (String) exchange.getAttributes().getOrDefault(REQUEST_LANGUAGE_ATTR, LanguageConstant.JAVA);

        return language;
    }

    public static Boolean getIsSkipAuth(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_SKIP_AUTH_ATTR, Boolean.FALSE);
    }

    public static Boolean getIsSkipUrlTamperProof(ServerWebExchange exchange) {
        return (Boolean) exchange.getAttributes().getOrDefault(IS_SKIP_URL_TAMPER_PROOF_ATTR, Boolean.FALSE);
    }

    public static RequestAddParamsDto getRequestAddParamsDto(ServerWebExchange exchange) {
        RequestAddParamsDto requestAddParamsDto = (RequestAddParamsDto) exchange.getAttributes().getOrDefault(GATEWAY_REQUEST_ADD_PARAMS_ATTR, null);
        if (requestAddParamsDto == null) {
            logger.warn("需要添加的参数为空");
            requestAddParamsDto = new RequestAddParamsDto();
            requestAddParamsDto.setUserId(null);
        }

        return requestAddParamsDto;
    }
}
