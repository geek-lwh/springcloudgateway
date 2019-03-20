package com.aha.tech.core.service;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;

import java.util.Set;

/**
 * @Author: luweihong
 * @Date: 2019/3/18
 */
public interface RuntimeConfigChangedService {

    /**
     * 路由API资源地址变更
     * @param changeEvent
     * @param changeKeys
     */
    void routeApiUriChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys);

    /**
     * 路由API白名单列表配置变更
     * @param changeEvent
     * @param changeKeys
     */
    void routeApiWhiteListChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys);

    /**
     * ip限流参数变更
     * @param changeEvent
     * @param changeKeys
     */
    void ipRateLimiterChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys);

}
