package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.exception.MissHeaderXForwardedException;
import com.aha.tech.core.model.enums.WebClientTypeEnum;
import com.aha.tech.core.service.ModifyHeaderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

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

    private static final String DEFAULT_VERSION = "10";

    private static final String DEFAULT_OS = "web";

    private static final String STR_PREFIX = "ahaschool";

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 初始化http请求头报文
     * @param httpHeaders
     */
    @Override
    public void initHeaders(HttpHeaders httpHeaders) {
        MediaType mediaType = httpHeaders.getContentType();
        if(mediaType == null){
            httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        }
        String realIp = parseHeaderIp(httpHeaders);
        if (StringUtils.isBlank(realIp)) {
            throw new MissHeaderXForwardedException();
        }

        httpHeaders.set(HEADER_X_FORWARDED_FOR, realIp);
        httpHeaders.set(HEADER_TOKEN, DEFAULT_X_TOKEN_VALUE);
        httpHeaders.add(HEADER_OS, DEFAULT_OS);
        httpHeaders.add(HEADER_VERSION, DEFAULT_VERSION);
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
            String value = userAgent.get(0).toLowerCase();
            int index = value.indexOf(STR_PREFIX);
            if (index == -1) {
                return;
            }

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
     * 设置后端rs需要的头部报文X-Env
     *
     * @param httpHeaders
     */
    @Override
    public void xEnvSetting(HttpHeaders httpHeaders) {
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
                xEnvSetting(key,value,httpHeaders);
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
    private void xEnvSetting(String key,String value,HttpHeaders httpHeaders){
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
            case X_ENV_FIELD_G_UNIQID:
                httpHeaders.set(HEADER_GUNIQID, value);
                break;
            case X_ENV_FIELD_CHANNEL:
                httpHeaders.set(X_ENV_CHANNEL, value);
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
        httpHeaders.remove(HEADER_REFERER);
        httpHeaders.remove(HEADER_ORIGIN);
        httpHeaders.remove(HEADER_X_REQUEST_PAGE);
        httpHeaders.remove(HEADER_HOST);
        httpHeaders.remove(HEADER_DNT);
        httpHeaders.remove(HEADER_COOKIE);
        httpHeaders.remove(HEADER_AUTHORIZATION);
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
     * var pp_raw = product_id:user_id:is_poster: group_id:open_group:poster_id
     * pp = pp_raw + '$' + 'pp' +md5("hjm?" + md5(string({"pp":pp_raw}) + "Aha^_^")
     * @param encodePP
     * @param httpHeaders
     */
    private void parseAndSetPp(String encodePP, HttpHeaders httpHeaders) {
        if (StringUtils.isBlank(encodePP)) {
            httpHeaders.add(HEADER_PP, Strings.EMPTY);
            return;
        }

        String pp = new String(Base64.decodeBase64(encodePP), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(pp) || !pp.contains(Separator.AND_MARK)) {
            httpHeaders.add(HEADER_PP, Strings.EMPTY);
            return;
        }

        String v = pp.substring(0, pp.indexOf(Separator.AND_MARK));

        if (!verifyPp(pp)) {
            logger.error("pp验证不通过!");
            httpHeaders.add(HEADER_PP, Strings.EMPTY);
            return;
        }

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
