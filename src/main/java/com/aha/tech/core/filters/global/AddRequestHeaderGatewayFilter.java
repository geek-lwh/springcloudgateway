package com.aha.tech.core.filters.global;

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

import static com.aha.tech.core.constant.FilterOrderedConstant.GLOBAL_ADD_REQUEST_HEADER_GATEWAY_FILTER;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 */
@Component
public class AddRequestHeaderGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AddRequestHeaderGatewayFilter.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return GLOBAL_ADD_REQUEST_HEADER_GATEWAY_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();

        ServerHttpRequest newRequest = buildNewRequest(serverHttpRequest);

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 构建新的请求体
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest buildNewRequest(ServerHttpRequest serverHttpRequest) {
        //X-Env
        HttpHeaders httpHeaders = new HttpHeaders();
        copyMultiValueMap(serverHttpRequest.getHeaders(), httpHeaders);


        List<String> userAgent = httpHeaders.get(HEADER_USER_AGENT);

        parseAuthorization(httpHeaders);
        parseXEnv(httpHeaders);

        serverHttpRequest = new ServerHttpRequestDecorator(serverHttpRequest) {
            @Override
            public HttpHeaders getHeaders() {
                return httpHeaders;
            }
        };

        return serverHttpRequest;
    }

    /**
     * 解析header中的 authorization
     * @param httpHeaders
     */
    private void parseAuthorization(HttpHeaders httpHeaders) {
        List<String> authorization = httpHeaders.get(HEADER_AUTHORIZATION);
        if (CollectionUtils.isEmpty(authorization)) {
            return;
        }

//        String token = authorization.get(0);

    }

    /**
     * 解析header中的xEnv
     * @param httpHeaders
     */
    private void parseXEnv(HttpHeaders httpHeaders) {
        List<String> xEnv = httpHeaders.get(HEADER_X_ENV);

        if (CollectionUtils.isEmpty(xEnv)) {
            return;
        }

        byte[] decryptXEnv = Base64.decodeBase64(xEnv.get(0));
        try {
            Map<String, Object> xEnvMap = objectMapper.readValue(decryptXEnv, Map.class);
            for (Map.Entry<String, Object> entry : xEnvMap.entrySet()){
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                switch (key) {
                    case X_ENV_FIELD_PK:
                        parseAndSetHeaderFiledOfPK(value, httpHeaders);
                        break;
                    case X_ENV_FIELD_PP:
                        parseAndSetHeaderFiledOfPP(value, httpHeaders);
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
                    case X_ENV_FIELD_APP_TYPE :
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
    private void parseAndSetHeaderFiledOfPK(String encodePK, HttpHeaders httpHeaders) {
        if (StringUtils.isBlank(encodePK)) {
            httpHeaders.set(HEADER_PK, Strings.EMPTY);
            return;
        }

        String pk = new String(Base64.decodeBase64(encodePK), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(pk) || !pk.startsWith("/")) {
            httpHeaders.set(HEADER_PK, Strings.EMPTY);
            return;
        }

        httpHeaders.set(HEADER_PK, pk);
    }

    /**
     * 解析header 并且设置 header 头字段 pp
     * @param encodePP
     * @param httpHeaders
     */
    private void parseAndSetHeaderFiledOfPP(String encodePP, HttpHeaders httpHeaders) {
        if (StringUtils.isBlank(encodePP)) {
            httpHeaders.set(HEADER_PP, Strings.EMPTY);
            return;
        }

        String pp = new String(Base64.decodeBase64(encodePP), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(pp) || !pp.contains("!")) {
            httpHeaders.set(HEADER_PP, Strings.EMPTY);
            return;
        }

        String v = pp.substring(0, pp.indexOf("!"));
        httpHeaders.set(HEADER_PP, v);
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
        if(k.length() > 1){
            return String.format("X-Env-%s%s", Character.toUpperCase(k.charAt(0)), k.substring(1));
        }

        return k;
    }

}
