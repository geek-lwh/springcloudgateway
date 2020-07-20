package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.constant.SystemConstant;
import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.service.ModifyHeaderService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.VersionSupport;
import com.dianping.cat.Cat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR;
import static com.aha.tech.core.constant.HeaderFieldConstant.*;
import static com.aha.tech.core.support.ParseHeadersSupport.parseHeaderIp;
import static com.aha.tech.core.support.ParseHeadersSupport.verifyPp;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 *
 * 修改头部信息
 */
@Service("httpModifyHeaderService")
public class HttpModifyHeaderServiceImpl implements ModifyHeaderService {

    private static final Logger logger = LoggerFactory.getLogger(HttpModifyHeaderServiceImpl.class);

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 初始化http请求头报文
     * @param exchange
     * @param httpHeaders
     * @param remoteIp
     */
    @Override
    public void initHeaders(ServerWebExchange exchange, HttpHeaders httpHeaders, String remoteIp) {
        String realIp = parseHeaderIp(httpHeaders);
        if (StringUtils.isBlank(realIp)) {
            String url = exchange.getAttributeOrDefault(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, "");
            logger.warn("url : {} 缺失x-forward-for , 使用 remoteIp : {}", url, remoteIp);
            realIp = remoteIp;
        }

        RequestAddParamsDto requestAddParamsDto = ExchangeSupport.getRequestAddParamsDto(exchange);
        String userId = requestAddParamsDto.getUserId() == null ? null : requestAddParamsDto.getUserId().toString();
        httpHeaders.set(X_ENV_USER_ID, userId);
        httpHeaders.set(HEADER_USER_ID, userId);

        httpHeaders.set(X_TRACE_ID, Cat.createMessageId());
        httpHeaders.set(CONSUMER_SERVER_NAME, Cat.getManager().getDomain());

        httpHeaders.set(HEADER_X_FORWARDED_FOR, realIp);
        httpHeaders.set(HEADER_TOKEN, DEFAULT_X_TOKEN_VALUE);
        httpHeaders.set(HEADER_OS, SystemConstant.WEB_CLIENT);
        httpHeaders.set(HEADER_VERSION, SystemConstant.DEFAULT_VERSION);
        httpHeaders.set(HEADER_KEEP_ALIVE, HEADER_KEEP_ALIVE_VALUE);
    }

    /**
     * 设置后端rs需要的头部信息version
     * @See junit demo ModifyRequestHeadersTest.java
     * @param httpHeaders
     */
    @Override
    public void versionSetting(HttpHeaders httpHeaders) {
        List<String> userAgent = httpHeaders.get(HEADER_USER_AGENT);
        if (CollectionUtils.isEmpty(userAgent)) {
            return;
        }

        try {
            String[] pair = VersionSupport.parseOsAndVersion(userAgent.get(0));
            httpHeaders.set(HEADER_OS, pair[0]);
            httpHeaders.set(HEADER_VERSION, pair[1]);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    /**
     * 设置后端rs需要的头部报文X-Env
     *
     * @param httpHeaders
     */
    @Override
    public void xEnvSetting(ServerWebExchange serverWebExchange, HttpHeaders httpHeaders) {
        List<String> xEnv = httpHeaders.get(HEADER_X_ENV);
        if (CollectionUtils.isEmpty(xEnv)) {
            return;
        }
        String url = serverWebExchange.getAttributeOrDefault(GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR, "");
        byte[] decryptXEnv = Base64.decodeBase64(xEnv.get(0));
        try {
            Map<String, Object> xEnvMap = objectMapper.readValue(decryptXEnv, Map.class);
            for (Map.Entry<String, Object> entry : xEnvMap.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                xEnvSetting(url, key, value, httpHeaders);
            }
        } catch (IOException e) {
            logger.error("parse core error", e);
        }
    }

    /**
     * 设置xEnv
     * @param key
     * @param value
     * @param httpHeaders
     */
    private void xEnvSetting(String url, String key, String value, HttpHeaders httpHeaders) {
        switch (key) {
            case X_ENV_FIELD_PK:
                parseAndSetPk(value, httpHeaders);
                break;
            case X_ENV_FIELD_PP:
                parseAndSetPP(url, value, httpHeaders);
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
            case X_ENV_FIELD_G_UNIQID:
                httpHeaders.set(HEADER_GUNIQID, value);
                break;
            case X_ENV_FIELD_CHANNEL:
                httpHeaders.set(X_ENV_CHANNEL, value);
                break;
            case X_ENV_FIELD_USER_ID:
                httpHeaders.set(X_ENV_USER_ID, value);
                break;
            default:
                parseDefault(key, value, httpHeaders);
                break;
        }
    }

    /**
     * 删除后端rs不需要的报文头,减少传输报文传输大小
     * @param httpHeaders
     */
    @Override
    public void removeHeaders(HttpHeaders httpHeaders) {
        httpHeaders.remove(HEADER_PRAGMA);
        httpHeaders.remove(HEADER_CACHE_CONTROL);
        httpHeaders.remove(HEADER_X_ENV);
        // httpHeaders.remove(HEADER_REFERER);
        httpHeaders.remove(HEADER_ORIGIN);
        httpHeaders.remove(HEADER_X_REQUEST_PAGE);
        httpHeaders.remove(HEADER_HOST);
        httpHeaders.remove(HEADER_DNT);
        httpHeaders.remove(HEADER_COOKIE);
        httpHeaders.remove(HEADER_AUTHORIZATION);

        // 删除url防篡改
        httpHeaders.remove(HEADER_X_CA_TIMESTAMP);
        httpHeaders.remove(HEADER_X_CA_SIGNATURE);
        httpHeaders.remove(HEADER_X_CA_VERSION);
        httpHeaders.remove(HEADER_X_CA_CONTENT);
        httpHeaders.remove(HEADER_X_CA_NONCE);
    }

    /**
     * 解析header 并且设置 header 头字段 pk
     * @param encodePK
     * @param httpHeaders
     */
    private void parseAndSetPk(String encodePK, HttpHeaders httpHeaders) {
        if (StringUtils.isBlank(encodePK)) {
            httpHeaders.set(HEADER_PK, Strings.EMPTY);
            return;
        }

        String pk = new String(Base64.decodeBase64(encodePK), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(pk) || !pk.startsWith(Separator.SLASH_MARK)) {
            httpHeaders.set(HEADER_PK, Strings.EMPTY);
            return;
        }

        httpHeaders.set(HEADER_PK, pk);
    }

    /**
     * 解析header 并且设置 header 头字段 pp
     * var pp_raw = product_id:user_id:is_poster: group_id:open_group:poster_id
     * pp = pp_raw + '$' + 'pp' +md5("hjm?" + md5(string({"pp":pp_raw}) + "Aha^_^")
     * pp = 505069:100325:0:6289:1:0$pp85de92771715f7782ff4cfc89141
     * @param encodePP
     * @param httpHeaders
     */
    private void parseAndSetPP(String url, String encodePP, HttpHeaders httpHeaders) {
        if (StringUtils.isBlank(encodePP)) {
            httpHeaders.set(HEADER_PP, Strings.EMPTY);
            return;
        }

        String pp = new String(Base64.decodeBase64(encodePP), StandardCharsets.UTF_8);
        if (!pp.contains(Separator.DOLLAR_MARK)) {
            logger.warn("url : {} ,httpHeaders : {} 不合法的pp值,缺少'$'符号 pp : {},encode_pp : {},pp.length = {}", url, httpHeaders.toSingleValueMap().toString(),pp, encodePP, pp.length());
            httpHeaders.set(HEADER_PP, Strings.EMPTY);
            return;
        }

        if (!verifyPp(pp)) {
            logger.warn("url : {} httpHeader : {} pp验证不通过! pp : {},encode_pp : {}", url, httpHeaders.toSingleValueMap().toString(),pp, encodePP);
            httpHeaders.set(HEADER_PP, Strings.EMPTY);
            return;
        }

        String v = pp.substring(0, pp.indexOf(Separator.DOLLAR_MARK));
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
        if (k.length() > 1) {
            return String.format("X-Env-%s%s", Character.toUpperCase(k.charAt(0)), k.substring(1));
        }

        return k;
    }
}
