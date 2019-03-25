package com.aha.tech.util;

import com.aha.tech.commons.symbol.Separator;

/**
 * @Author: monkey
 * @Date: 2018/7/28
 *
 * redis key 定义类
 */
public class KeyGenerateUtil {

    // 项目的namespace
    public static final String NAMESPACE = "gateway";


    /**
     * 某个具体业务的redis key
     */
    public static class GatewayLimiter {

        // 全局访问限制
        public static String QPS_LIMITER_PREFIX = "QPS_LIMITER_PREFIX";

        /**
         * 全局访问限制 key
         * @return
         */
        public static String qpsLimiterKey() {
            return NAMESPACE + Separator.COLON_MARK + QPS_LIMITER_PREFIX;
        }
    }
}
