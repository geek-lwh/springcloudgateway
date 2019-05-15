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
     * 跳过授权模块白名单
     * @param changeEvent
     * @param changeKeys
     */
    void skipAuthWhiteListChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys);

    /**
     * 跳过ip限流白名单
     * @param changeEvent
     * @param changeKeys
     */
    void skipIpLimiterWhiteListChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys);
}
