package com.aha.tech.core.constant;

import com.google.common.collect.Lists;
import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 */
public class HeaderFieldConstant {

    public static final String HEADER_SKIP_URL_TAMPER_PROOF = "X-NO-TAMPER-PROOF";

    public static final String HEADER_CONNECTION = "Connection";

    public static final String HEADER_KEEP_ALIVE = "Keep-Alive";

    public static final String HEADER_KEEP_ALIVE_VALUE = "timeout=30";

    public static final String KEEP_ALIVE_VALUE = "keep-alive";

    public static final String CLOSE_VALUE = "close";


    public static final String HEADER_TOKEN = "X-Token";

    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    public static final String DEFAULT_X_TOKEN_VALUE = "28ad87ef9fdce5d12dea093b860e8772";

    public static final String DEFAULT_ACCEPT_ENCODING = "gzip, deflate, br";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_HOST = "Host";

    public static final String HEADER_DNT = "DNT";

    public static final String HEADER_COOKIE = "Cookie";

    public static final String HEADER_USER_AGENT = "User-Agent";

    public static final String HEADER_X_REQUEST_PAGE = "X-Request-Page";

    public static final String HEADER_X_ENV = "X-Env";

    public static final String X_ENV_FIELD_UTM_SOURCE = "utm_source";

    public static final String HEADER_UTM_SOURCE = "X-Env-Utm-Source";

    public static final String X_ENV_FIELD_UTM_MEDIUM = "utm_medium";

    public static final String HEADER_UTM_MEDIUM = "X-Env-Utm-Medium";

    public static final String X_ENV_FIELD_UTM_CAMPAIGN = "utm_campaign";

    public static final String HEADER_UTM_CAMPAIGN = "X-Env-Utm-Campaign";

    public static final String X_ENV_FIELD_UTM_TERM = "utm_term";

    public static final String HEADER_UTM_TERM = "X-Env-Utm-Term";

    public static final String X_ENV_FIELD_UTM_CONTENT = "utm_content";

    public static final String HEADER_UTM_CONTENT = "X-Env-Utm-Content";

    public static final String X_ENV_FIELD_PK = "pk";

    public static final String HEADER_PK = "X-Env-PK";

    public static final String X_ENV_FIELD_PD = "pd";

    public static final String HEADER_PD = "X-Env-PD";

    public static final String X_ENV_FIELD_PS = "ps";

    public static final String HEADER_PS = "X-Env-PS";

    public static final String X_ENV_FIELD_PP = "pp";

    public static final String HEADER_PP = "X-Env-PP";

    public static final String X_ENV_FIELD_APP_TYPE = "app_type";

    public static final String HEADER_APP_TYPE = "X-Env-App-Type";

    public static final String X_ENV_FIELD_G_UNIQID = "guniqid";

    public static final String HEADER_GUNIQID = "X-Env-Guniqid";

    public static final String X_ENV_FIELD_CHANNEL = "channel";

    public static final String X_ENV_FIELD_USER_ID = "user_id";

    public static final String HEADER_USER_ID = "user_id";

    public static final String X_ENV_USER_ID = "X-Env-User-Id";

    public static final String REQUEST_ID = "request_id";

    public static final String X_TRACE_ID = "X-Trace-Id";

    public static final String CONSUMER_SERVER_NAME = "X-Consumer-Server-Name";

    public static final String X_ENV_CHANNEL = "X-Env-Channel";


    public static final String HEADER_PRAGMA = "Pragma";

    public static final String HEADER_CACHE_CONTROL = "cache-control";

    public static final String HEADER_REFERER = "Referer";

    public static final String HEADER_ORIGIN = "Origin";

    public static final String HEADER_VERSION = "version";

    public static final String HEADER_OS = "os";

    public static final String HEADER_ALL_CONTROL_ALLOW_ORIGIN_ACCESS = "*";

    public static final long HEADER_CROSS_ACCESS_ALLOW_MAX_AGE = 60l * 60 * 24 * 7;

    public static final List<HttpMethod> HEADER_CROSS_ACCESS_ALLOW_HTTP_METHODS = Lists.newArrayList(HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE, HttpMethod.PUT);

    public static final List<String> HEADER_CROSS_ACCESS_ALLOW_ALLOW_HEADERS = Lists.newArrayList("Authorization", "Origin", "X-Requested-With", "X-Env", "X-Request-Page", "Content-Type", "Accept", "X-Ca-Timestamp", "X-Ca-Nonce", "X-Ca-Signature", "X-Ca-Version", "X-Ca-Content");

    public static final String HEADER_X_CA_NONCE = "X-Ca-Nonce";

    // 时间戳
    public static final String HEADER_X_CA_TIMESTAMP = "X-Ca-Timestamp";

    // 签名
    public static final String HEADER_X_CA_SIGNATURE = "X-Ca-Signature";

    // 版本
    public static final String HEADER_X_CA_VERSION = "X-Ca-Version";

    // body签名
    public static final String HEADER_X_CA_CONTENT = "X-Ca-Content";

    // X-Ca-Version 中的值 代表版本号
    public static final String VERSION_FROYO = "Froyo";
}
