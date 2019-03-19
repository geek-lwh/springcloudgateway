package com.aha.tech.core.constant;

import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;

import static org.springframework.cloud.gateway.filter.NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 * 定义gateway的执行顺序
 */
public class GatewayFilterProcessOrderedConstant {

    public static int STEP = 20;

    // 重写请求路径过滤器 order = 0
    public static int GLOBAL_REWRITE_REQUEST_PATH_FILTER_ORDER = 1;

    // 全新啊校验过滤器 order = 21
    public static int GLOBAL_AUTH_GATEWAY_FILTER_ORDER = GLOBAL_REWRITE_REQUEST_PATH_FILTER_ORDER + STEP;

    // 修改请求报头过滤器 order = 41
    public static int GLOBAL_MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER = GLOBAL_AUTH_GATEWAY_FILTER_ORDER + STEP;

    // 修改请求返回体过滤器 order = -21
    public static int GLOBAL_MODIFY_RESPONSE_BODY_GATEWAY_FILTER_ORDER = WRITE_RESPONSE_FILTER_ORDER -  STEP;

    // 修改请求返回报头过滤器 order = -20
    public static int GLOBAL_MODIFY_RESPONSE_HEADER_GATEWAY_FILTER_ORDER = GLOBAL_MODIFY_RESPONSE_BODY_GATEWAY_FILTER_ORDER - STEP;
}
