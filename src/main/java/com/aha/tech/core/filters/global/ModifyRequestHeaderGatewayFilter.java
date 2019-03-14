package com.aha.tech.core.filters.global;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.model.enums.WebClientTypeEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.aha.tech.core.constant.FilterOrderedConstant.GLOBAL_ADD_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 */
@Component
public class ModifyRequestHeaderGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestHeaderGatewayFilter.class);

    private static final String DEFAULT_VERSION = "10";

    private static final String DEFAULT_OS = "web";

    private static final String STR_PREFIX = "ahaschool";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return GLOBAL_ADD_REQUEST_HEADER_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();

        ServerHttpRequest newRequest = modifyRequestHeader(serverHttpRequest);
        logger.debug("after modify request header : {}", newRequest.getHeaders());
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 构建新的请求体
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest modifyRequestHeader(ServerHttpRequest serverHttpRequest) {
        HttpHeaders httpHeaders = new HttpHeaders();
        copyMultiValueMap(serverHttpRequest.getHeaders(), httpHeaders);

        modifyRequestHttpHeader(httpHeaders);
        serverHttpRequest = new ServerHttpRequestDecorator(serverHttpRequest) {
            @Override
            public HttpHeaders getHeaders() {
                return httpHeaders;
            }
        };

        return serverHttpRequest;
    }

    /**
     * 修改转发的头信息
     * @param httpHeaders
     */
    private void modifyRequestHttpHeader(HttpHeaders httpHeaders) {
        parseUserAgentAndModifyHeader(httpHeaders);
        parseXEnvAndModifyHeader(httpHeaders);
        initRequestHeader(httpHeaders);
        removeHeader(httpHeaders);
    }

    /**
     * 解析user-anget头对象 并且设置http header头信息
     * todo ios 审核版本判断需要app小组自己实现
     * @param httpHeaders
     */
    private void parseUserAgentAndModifyHeader(HttpHeaders httpHeaders) {
        List<String> userAgent = httpHeaders.get(HEADER_USER_AGENT);
        if (CollectionUtils.isEmpty(userAgent)) {
            return;
        }

        try {
            String value = userAgent.get(0).toLowerCase();
            int index = value.indexOf(STR_PREFIX);
            if (index == -1) {
                return;
            }

            // 截图出现的prefix到末尾
            String subStr = value.substring(index);
            String[] arr = StringUtils.split(subStr, Separator.SLASH_MARK);
            String os = arr[1];
            String version = arr[2];
            if (os.equals(WebClientTypeEnum.ANDROID.getName()) || os.equals(WebClientTypeEnum.IOS.getName())) {
                httpHeaders.add(HEADER_OS, os);
                httpHeaders.add(HEADER_VERSION, version);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * 删除不必要的头信息
     * 减少http header 大小
     * @param httpHeaders
     */
    private void removeHeader(HttpHeaders httpHeaders) {
        httpHeaders.remove(HEADER_PRAGMA);
        httpHeaders.remove(HEADER_CACHE_CONTROL);
        httpHeaders.remove(HEADER_X_ENV);
        httpHeaders.remove(HEADER_REFERER);
        httpHeaders.remove(HEADER_ORIGIN);
        httpHeaders.remove(HEADER_USER_AGENT);
        httpHeaders.remove(HEADER_X_REQUEST_PAGE);
        httpHeaders.remove(HEADER_HOST);
        httpHeaders.remove(HEADER_DNT);
        httpHeaders.remove(HEADER_COOKIE);
        httpHeaders.remove(HEADER_AUTHORIZATION);
    }

    /**
     * 添加头信息
     * @param httpHeaders
     */
    private void initRequestHeader(HttpHeaders httpHeaders) {
        httpHeaders.set(HEADER_TOKEN, DEFAULT_X_TOKEN_VALUE);
        httpHeaders.add(HEADER_OS, DEFAULT_OS);
        httpHeaders.add(HEADER_VERSION, DEFAULT_VERSION);
    }


    /**
     * 解析header中的xEnv
     * @param httpHeaders
     */
    private void parseXEnvAndModifyHeader(HttpHeaders httpHeaders) {
        List<String> xEnv = httpHeaders.get(HEADER_X_ENV);

        if (CollectionUtils.isEmpty(xEnv)) {
            return;
        }

        byte[] decryptXEnv = Base64.decodeBase64(xEnv.get(0));
        try {
            Map<String, Object> xEnvMap = objectMapper.readValue(decryptXEnv, Map.class);
            for (Map.Entry<String, Object> entry : xEnvMap.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                switch (key) {
                    case X_ENV_FIELD_PK:
                        parseAndSetPk(value, httpHeaders);
                        break;
                    case X_ENV_FIELD_PP:
                        parseAndSetPp(value, httpHeaders);
                        break;
                    case X_ENV_FIELD_PD:
                        httpHeaders.set(HEADER_PD, value);
                        break;
                    case X_ENV_FIELD_PS:
                        httpHeaders.set(HEADER_PS, value);
                        break;
                    case X_ENV_FIELD_UTM_SOURCE:
                        httpHeaders.set(HEADER_UTM_SOURCE, value);
                        break;
                    case X_ENV_FIELD_UTM_MEDIUM:
                        httpHeaders.set(HEADER_UTM_MEDIUM, value);
                        break;
                    case X_ENV_FIELD_UTM_CAMPAIGN:
                        httpHeaders.set(HEADER_UTM_CAMPAIGN, value);
                        break;
                    case X_ENV_FIELD_UTM_TERM:
                        httpHeaders.set(HEADER_UTM_TERM, value);
                        break;
                    case X_ENV_FIELD_UTM_CONTENT:
                        httpHeaders.set(HEADER_UTM_CONTENT, value);
                        break;
                    case X_ENV_FIELD_APP_TYPE:
                        httpHeaders.set(HEADER_APP_TYPE, value);
                        break;
                    case X_ENV_FIELD_GUNIQID:
                        httpHeaders.set(HEADER_GUNIQID, value);
                        break;
                    default:
                        parseDefault(key, value, httpHeaders);
                        break;
                }
            }
        } catch (IOException e) {
            logger.error("parse xEnv error", e);
        }
    }

    /**
     * 解析header 并且设置 header 头字段 pk
     * @param encodePK
     * @param httpHeaders
     */
    private void parseAndSetPk(String encodePK, HttpHeaders httpHeaders) {
        if (StringUtils.isBlank(encodePK)) {
            httpHeaders.add(HEADER_PK, Strings.EMPTY);
            return;
        }

        String pk = new String(Base64.decodeBase64(encodePK), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(pk) || !pk.startsWith(Separator.SLASH_MARK)) {
            httpHeaders.add(HEADER_PK, Strings.EMPTY);
            return;
        }

        httpHeaders.add(HEADER_PK, pk);
    }

    /**
     * 解析header 并且设置 header 头字段 pp
     * @param encodePP
     * @param httpHeaders
     */
    private void parseAndSetPp(String encodePP, HttpHeaders httpHeaders) {
        if (StringUtils.isBlank(encodePP)) {
            httpHeaders.add(HEADER_PP, Strings.EMPTY);
            return;
        }

        String pp = new String(Base64.decodeBase64(encodePP), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(pp) || !pp.contains("!")) {
            httpHeaders.add(HEADER_PP, Strings.EMPTY);
            return;
        }

        String v = pp.substring(0, pp.indexOf("!"));
        httpHeaders.add(HEADER_PP, v);
    }

    /**
     * 默认的解析方案
     * @param key
     * @param value
     * @param httpHeaders
     */
    private void parseDefault(String key, String value, HttpHeaders httpHeaders) {
        String headerKey = formatXEnvHeaderKey(key);
        String headerValue = StringUtils.isBlank(value) ? Strings.EMPTY : value;
        httpHeaders.set(headerKey, headerValue);
    }

    /**
     * 与后端约定的xEnv头格式
     * X-Env- + k(首字母大写)
     * @param k
     * @return
     */
    private static String formatXEnvHeaderKey(String k) {
        if (k.length() > 1) {
            return String.format("X-Env-%s%s", Character.toUpperCase(k.charAt(0)), k.substring(1));
        }

        return k;
    }

}
