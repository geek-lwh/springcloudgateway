package com.aha.tech.core.event;

import com.aha.tech.config.RouteConfiguration;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.util.SpringContextUtil;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @Author: luweihong
 * @Date: 2018/9/19
 *
 * 用于监听一些变量变更后的动作,如果无需后续动作可以不更新
 * apollo实时更新
 */
@Component
public class VariableRefreshEvent {

    private static final Logger logger = LoggerFactory.getLogger(VariableRefreshEvent.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshScope refreshScope;

    private String DYNAMIC_ROUTE_BEAN = "routeEntityMap";

    private String ROUTE_PREFIX = "route.mappings.";


    /**
     * apollo 配置变更
     *
     * @param changeEvent
     */
    @ApolloConfigChangeListener({"application"})
    private void configChangeListener(ConfigChangeEvent changeEvent) {
        logger.info("配置中心 配置被发布!");
        Set<String> changeKeys = changeEvent.changedKeys();
        changeKeys.forEach(changeKeyName -> {
            logger.info("变更的配置名 : {}", changeKeyName);
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.startsWith(ROUTE_PREFIX)) {
                runtimeRouteConfigChangeListener(change, changeKeyName);
            }
        });
    }

    /**
     * 监听运行时路由配置变更
     *
     * @param change
     * @param changeKeyName
     */
    private void runtimeRouteConfigChangeListener(ConfigChange change, String changeKeyName) {
        logger.info("网关路由配置变更 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        Map<String, RouteEntity> routeEntityMap = (Map<String, RouteEntity>) SpringContextUtil.getBean(DYNAMIC_ROUTE_BEAN);
        String key = changeKeyName.substring(ROUTE_PREFIX.length());
        if (!routeEntityMap.containsKey(key)) {
            RouteEntity routeEntity = parseMapping(newValue);
            routeEntityMap.put(key, routeEntity);
            refreshRuntimeRouteConfig();
            logger.info("添加 路由配置映射关系 : {}", ROUTE_PREFIX + key);
            return;
        }

        if (StringUtils.isBlank(newValue)) {
            routeEntityMap.remove(key);
            refreshRuntimeRouteConfig();
            logger.info("删除 路由配置映射关系 : {}", ROUTE_PREFIX + key);
            return;
        }

        routeEntityMap.put(key, parseMapping(newValue));
        refreshRuntimeRouteConfig();
        logger.info("修改 路由配置映射关系 : {}", ROUTE_PREFIX + key);
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

    /**
     * 刷新路由映射配置
     */
    private void refreshRuntimeRouteConfig() {
        for(;;){
            if(RouteConfiguration.routeChange.compareAndSet(false,true)){
                refreshScope.refresh(DYNAMIC_ROUTE_BEAN);
                break;
            }
        }
    }

}
