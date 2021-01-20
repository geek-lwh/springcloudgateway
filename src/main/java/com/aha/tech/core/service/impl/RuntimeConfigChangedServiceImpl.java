package com.aha.tech.core.service.impl;

import com.aha.tech.config.RouteConfiguration;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.service.RuntimeConfigChangedService;
import com.aha.tech.util.SpringContextUtil;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO yao remove
 * @Author: luweihong
 * @Date: 2019/3/18
 */
@Service("runtimeConfigChangedService")
public class RuntimeConfigChangedServiceImpl implements RuntimeConfigChangedService {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigChangedServiceImpl.class);

    private String DYNAMIC_ROUTE_API_URI_BEAN = "routeEntityMap";

    private String DYNAMIC_AUTH_WHITE_LIST_BEAN = "authWhiteList";

    private String DYNAMIC_IP_LIMITER_WHITE_LIST_BEAN = "ipLimiterWhiteList";

    private String DYNAMIC_URL_TAMPER_PROOF_WHITE_LIST_BEAN = "urlTamperProofWhiteList";

    private String ROUTE_API_URI_PREFIX = "route.api.uri.mappings.";

    private String SKIP_AUTH_WHITE_LIST_PREFIX = "skip.auth.white.list";

    private String SKIP_IP_LIMITER_WHITE_LIST_PREFIX = "skip.iplimiter.white.list";

    private String SKIP_URL_TAMPER_PROOF_WHITE_LIST_PREFIX = "skip.url.tamper.proof.white.list";


    @Autowired
    private ObjectMapper objectMapper;

    @Resource
    private RefreshScope refreshScope;

    /**
     * 路由API资源地址变更
     * @param changeEvent
     * @param changeKeys
     */
    @Override
    public synchronized void routeApiUriChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys) {
        changeKeys.forEach(changeKeyName -> {
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.startsWith(ROUTE_API_URI_PREFIX)) {
                refreshRouteApiUriConfig(change, ROUTE_API_URI_PREFIX, changeKeyName);
            }

        });

        for (; ; ) {
            if (RouteConfiguration.needToSyncLocal.compareAndSet(false, true)) {
                logger.info("路由更新变化,刷新缓存成功");
                break;
            }

            logger.warn("路由参数变化,没有更新缓存,强制缓存更新!");
            RouteConfiguration.needToSyncLocal.compareAndSet(true, false);
        }
    }

    /**
     * 跳过授权白名单列表变更
     * @param changeEvent
     * @param changeKeys
     */
    @Override
    public synchronized void skipAuthWhiteListChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys) {
        changeKeys.forEach(changeKeyName -> {
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.startsWith(SKIP_AUTH_WHITE_LIST_PREFIX)) {
                refreshSkipAuthWhiteListConfig(change);
            }
        });
    }

    /**
     * 跳过ip限流白名单列表变更
     * @param changeEvent
     * @param changeKeys
     */
    @Override
    public void skipIpLimiterWhiteListChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys) {
        changeKeys.forEach(changeKeyName -> {
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.startsWith(SKIP_IP_LIMITER_WHITE_LIST_PREFIX)) {
                refreshSkipIpLimiterWhiteListConfig(change);
            }
        });
    }

    /**
     * 跳过url防篡改白名单列表变更
     * @param changeEvent
     * @param changeKeys
     */
    @Override
    public void skipUrlTamperProofWhiteListChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys) {
        changeKeys.forEach(changeKeyName -> {
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.startsWith(SKIP_URL_TAMPER_PROOF_WHITE_LIST_PREFIX)) {
                refreshSkipUrlTamperProofWhiteListConfig(change);
            }
        });
    }

    /**
     * 刷新路由API资源配置
     * @param change
     * @param prefix
     * @param changeKeyName
     */
    private void refreshRouteApiUriConfig(ConfigChange change, String prefix, String changeKeyName) {
        logger.info("路由API资源地址变更 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        Map<String, RouteEntity> routeEntityMap = (Map<String, RouteEntity>) SpringContextUtil.getBean(DYNAMIC_ROUTE_API_URI_BEAN);
        String key = changeKeyName.substring(prefix.length());

        if (StringUtils.isBlank(newValue)) {
            routeEntityMap.remove(key);
            refreshScope.refresh(DYNAMIC_ROUTE_API_URI_BEAN);
            logger.info("删除 路由API资源地址 : {}", prefix + key);
            return;
        }

        RouteEntity routeEntity = parseMapping(newValue);
        routeEntityMap.put(key, routeEntity);
        refreshScope.refresh(DYNAMIC_ROUTE_API_URI_BEAN);

        logger.info("更新 路由API资源地址 : {}", prefix + key);
    }


    /**
     * 刷新跳过授权白名单列表配置
     * @param change
     */
    private void refreshSkipAuthWhiteListConfig(ConfigChange change) {
        logger.info("刷新跳过授权白名单列表配置 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        List<String> authWhiteList = (List<String>) SpringContextUtil.getBean(DYNAMIC_AUTH_WHITE_LIST_BEAN);

        authWhiteList.remove(oldValue);
        authWhiteList.add(newValue);

        refreshScope.refresh(DYNAMIC_AUTH_WHITE_LIST_BEAN);

        logger.info("更新 刷新跳过授权白名单列表配置 : {}", authWhiteList);
    }

    /**
     * 刷新跳过ip限流白名单列表配置
     * @param change
     */
    private void refreshSkipIpLimiterWhiteListConfig(ConfigChange change) {
        logger.info("刷新跳过ip限流白名单列表配置 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        List<String> ipLimiterWhiteList = (List<String>) SpringContextUtil.getBean(DYNAMIC_IP_LIMITER_WHITE_LIST_BEAN);

        ipLimiterWhiteList.remove(oldValue);
        ipLimiterWhiteList.add(newValue);

        refreshScope.refresh(DYNAMIC_IP_LIMITER_WHITE_LIST_BEAN);

        logger.info("更新 刷新跳过ip限流白名单列表配置 : {}", ipLimiterWhiteList);
    }

    /**
     * 刷新跳过url防篡改白名单列表配置
     * @param change
     */
    private void refreshSkipUrlTamperProofWhiteListConfig(ConfigChange change) {
        logger.info("刷新跳过url防篡改白名单列表配置 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        List<String> urlTamperProofWhiteList = (List<String>) SpringContextUtil.getBean(DYNAMIC_URL_TAMPER_PROOF_WHITE_LIST_BEAN);

        urlTamperProofWhiteList.remove(oldValue);
        urlTamperProofWhiteList.add(newValue);

        refreshScope.refresh(DYNAMIC_URL_TAMPER_PROOF_WHITE_LIST_BEAN);

        logger.info("更新 刷新跳过url防篡改白名单列表配置 : {}", urlTamperProofWhiteList);
    }

    /**
     * json 解析字符串成对象
     *
     * @param newValue
     * @return
     */
    private RouteEntity parseMapping(String newValue) {
        RouteEntity routeEntity = null;
        try {
            routeEntity = objectMapper.readValue(newValue, RouteEntity.class);
        } catch (IOException e) {
            logger.error("解析json字符串异常,字符串 : {}", newValue, e);
        }
        return routeEntity;
    }

}
