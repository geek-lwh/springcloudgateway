package com.aha.tech.core.constant;

/**
 * @Author: luweihong
 * @Date: 2019/3/11
 *
 * 网关属性变量
 */
public class ExchangeAttributeConstant {

    // 访问请求id
    public static final String ACCESS_REQUEST_ID_ATTR = "ACCESS_REQUEST_ID_ATTR";

    // 访问请求时间
    public static final String ACCESS_REQUEST_TIME_ATTR = "ACCESS_REQUEST_TIME_ATTR";

    // 访问请求remoteIp
    public static final String ACCESS_REMOTE_IP_ATTR = "ACCESS_REMOTE_IP_ATTR";

    // 访问请求X-Forwarded-For
    public static final String ACCESS_X_FORWARDED_IP_ATTR = "ACCESS_X_FORWARDED_IP_ATTR";

    // 访问请求userName
    public static final String ACCESS_USER_NAME_ATTR = "ACCESS_USER_NAME_ATTR";

    // 访问请求cookies
    public static final String ACCESS_LOG_COOKIE_ATTR = "ACCESS_LOG_COOKIE_ATTR";

    public static final String ACCESS_LOG_ORIGINAL_URL_PATH_ATTR = "ACCESS_LOG_ORIGINAL_URL_PATH_ATTR";

    // 请求中有效的路径属性
    public static final String GATEWAY_REQUEST_VALID_PATH_ATTR = "GATEWAY_REQUEST_VALID_PATH_ATTR";

    // 重写后的路径地址属性
    public static final String GATEWAY_REQUEST_REWRITE_PATH_ATTR = "GATEWAY_REQUEST_REWRITE_PATH_ATTR";

    // routeId 匹配路由资源映射的key
    public static final String GATEWAY_REQUEST_ROUTE_ID_ATTR = "GATEWAY_REQUEST_ROUTE_ID_ATTR";

    // 网关请求时添加的参数属性
    public static final String GATEWAY_REQUEST_ADD_PARAMS_ATTR = "GATEWAY_REQUEST_ADD_PARAMS_ATTR";

}
