package com.aha.tech.core.constant;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 * 定义gateway的执行顺序
 */
public class FilterOrderedConstant {

    public static int GLOBAL_AUTH_GATEWAY_FILTER_ORDER = 200;

    public static int GLOBAL_ADD_REQUEST_HEADER_GATEWAY_FILTER_ORDER = 205;

    public static int GLOBAL_ADD_REQUEST_BODY_GATEWAY_FILTER_ORDER = 210;

    public static int GLOBAL_ADD_REQUEST_PARAMS_GATEWAY_FILTER_ORDER = 211;

    public static int GLOBAL_MODIFY_RESPONSE_GATEWAY_FILTER_ORDER = -200;
}
