package com.aha.tech.core.constant;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 * 定义gateway的执行顺序
 */
public class FilterProcessOrderedConstant {

    public static int STEP = 10;

    // 预处理过滤器
    public static int PRE_HANDLER_FILTER_ORDER = 0;

    // cpu使用率限流
    public static int CPU_RATE_LIMITER_FILTER_ORDER = PRE_HANDLER_FILTER_ORDER + STEP;

    // qps限流
    public static int QPS_RATE_LIMITER_FILTER_ORDER = CPU_RATE_LIMITER_FILTER_ORDER + STEP;

    // ip限流
    public static int IP_RATE_LIMITER_FILTER_ORDER = QPS_RATE_LIMITER_FILTER_ORDER + STEP;

    // 重写请求路径过滤器
    public static int REWRITE_REQUEST_PATH_FILTER_ORDER = IP_RATE_LIMITER_FILTER_ORDER + STEP;

    // 全新啊校验过滤器
    public static int AUTH_GATEWAY_FILTER_ORDER = REWRITE_REQUEST_PATH_FILTER_ORDER + STEP;

    // 修改请求报头过滤器
    public static int MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER = AUTH_GATEWAY_FILTER_ORDER + STEP;

    // 修改GET|POST请求的参数过滤器
    public static int MODIFY_PARAMS_FILTER_ORDER = MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER + STEP;

    // 修改请求返回体过滤器
    public static int MODIFY_RESPONSE_GATEWAY_FILTER_ORDER = MODIFY_PARAMS_FILTER_ORDER + STEP;


}
