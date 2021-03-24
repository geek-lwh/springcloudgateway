package com.aha.tech.core.constant;

/**
 * @Author: luweihong
 * @Date: 2019/3/11
 *
 * 网关属性变量
 */
public class AttributeConstant {

    // 原始路径
    public static final String GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR = "GATEWAY_REQUEST_ORIGINAL_URL_PATH_ATTR";

    public static final String GATEWAY_REQUEST_ROUTE_HOST_ATTR = "GATEWAY_REQUEST_ROUTE_HOST_ATTR";

    // 请求中有效的路径属性
    public static final String GATEWAY_REQUEST_VALID_PATH_ATTR = "GATEWAY_REQUEST_VALID_PATH_ATTR";

    // 重写后的路径地址属性
    public static final String GATEWAY_REQUEST_REWRITE_PATH_ATTR = "GATEWAY_REQUEST_REWRITE_PATH_ATTR";

    // routeId 匹配路由资源映射的key
    public static final String GATEWAY_REQUEST_ROUTE_ID_ATTR = "GATEWAY_REQUEST_ROUTE_ID_ATTR";

    // 网关请求时添加的参数属性
    public static final String GATEWAY_REQUEST_ADD_PARAMS_ATTR = "GATEWAY_REQUEST_ADD_PARAMS_ATTR";

    public static final String GATEWAY_SNAPSHOT_REQUEST_ATTR = "GATEWAY_REQUEST_CACHED_ATTR";

    public static final String IS_SKIP_AUTH_ATTR = "IS_SKIP_AUTH_ATTR";

    public static final String IS_IGNORE_5300_ERROR = "IS_IGNORE_5300_ERROR";

    public static final String IS_SKIP_URL_TAMPER_PROOF_ATTR = "IS_SKIP_URL_TAMPER_PROOF_ATTR";

    public static final String IS_SKIP_IP_LIMITER_ATTR = "IS_SKIP_IP_LIMITER_ATTR";

    public static final String REQUEST_IP_ATTR = "REQUEST_IP_ATTR";

    public static final String IS_OLD_VERSION_ATTR = "IS_OLD_VERSION_ATTR";

    public static final String IS_NEED_UPGRADE_ATTR = "IS_NEED_UPGRADE_ATTR";

    public static final String APP_OS_ATTR = "APP_OS_ATTR";

    public static final String APP_VERSION_ATTR = "APP_VERSION_ATTR";

    public static final String IP_LIMITER_ATTR = "IP_LIMITER_ATTR";

    public static final String RESPONSE_LINE = "RESPONSE_LINE";

    public static final String TRACE_LOG_ID = "TRACE_LOG_ID";

    public static final String ACTIVE_SPAN = "ACTIVE_SPAN";

    public static final String HTTP_STATUS = "HTTP_STATUS";

    public static final String USER_ID = "USER_ID";

    public static final String PARENT_SPAN = "PARENT_SPAN";

}
