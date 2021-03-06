package com.aha.tech.core.constant;

import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 * 定义gateway的执行顺序
 */
public class FilterProcessOrderedConstant {

    public static int STEP = 10;

    public static int ATTRIBUTE_SETTING_FILTER_ORDER = 0;

    // cpu使用率限流
    public static int CPU_RATE_LIMITER_FILTER_ORDER = ATTRIBUTE_SETTING_FILTER_ORDER + STEP;

    // qps限流
    public static int QPS_RATE_LIMITER_FILTER_ORDER = CPU_RATE_LIMITER_FILTER_ORDER + STEP;

    // ip限流
    public static int IP_RATE_LIMITER_FILTER_ORDER = QPS_RATE_LIMITER_FILTER_ORDER + STEP;

    // 校验和缓存过滤器
    public static int URL_TAMPER_PROOF_FILTER = IP_RATE_LIMITER_FILTER_ORDER + STEP;

    public static int SNAPSHOT_FILTER = URL_TAMPER_PROOF_FILTER + STEP;

    // 校验和缓存过滤器
    public static int BODY_TAMPER_PROOF_FILTER = SNAPSHOT_FILTER + STEP;

    // 权限校验
    public static int AUTH_GATEWAY_FILTER_ORDER = BODY_TAMPER_PROOF_FILTER + STEP;

    // 重写请求路径过滤器
    public static int REWRITE_REQUEST_PATH_FILTER_ORDER = AUTH_GATEWAY_FILTER_ORDER + STEP;

    // 修改请求报头过滤器 在重写host之后
    public static int MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER = REWRITE_REQUEST_PATH_FILTER_ORDER + STEP;

    // 修改GET|POST请求的参数过滤器
    public static int MODIFY_PARAMS_FILTER_ORDER = MODIFY_REQUEST_HEADER_GATEWAY_FILTER_ORDER + STEP;

    // loadbalance 过滤器
    public static int LOAD_BALANCING_FILTER = LoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + STEP;

    // 修改请求返回体过滤器
    public static int MODIFY_RESPONSE_GATEWAY_FILTER_ORDER = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - STEP;
//    public static int MODIFY_RESPONSE_GATEWAY_FILTER_ORDER = LOAD_BALANCING_FILTER + STEP;

}
