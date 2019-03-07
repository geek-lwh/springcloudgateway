package com.aha.tech.core.event;

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

    private String ROUTE_PREFIX = "route.mappings.";

    private String DYNAMIC_ROUTE_BEAN = "routeEntityMap";

    /**
     * apollo 配置变更
     * @param changeEvent
     */
    @ApolloConfigChangeListener({"application"})
    private void configChangeListener(ConfigChangeEvent changeEvent) {
        logger.info("配置中心 配置被发布!");
        Set<String> changeKeys = changeEvent.changedKeys();
        changeKeys.forEach(changeKeyName -> {
            if (changeKeyName.startsWith(ROUTE_PREFIX)) {
                logger.info("网关路由配置变更 !");
                ConfigChange change = changeEvent.getChange(changeKeyName);
                String oldValue = change.getOldValue();
                String newValue = change.getNewValue();
                logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

                RouteEntity routeEntity = null;
                Map<String, RouteEntity> routeEntityMap = (Map<String, RouteEntity>) SpringContextUtil.getBean(DYNAMIC_ROUTE_BEAN);
                String key = changeKeyName.substring(ROUTE_PREFIX.length());
                if(!routeEntityMap.containsKey(key)){
                    routeEntity = parseMapping(newValue);
                    routeEntityMap.put(key, routeEntity);
                    refreshRouteMapping();
                    logger.info("添加 路由配置映射关系 : {}", ROUTE_PREFIX + key);
                    return;
                }

                if (StringUtils.isBlank(newValue)) {
                    routeEntityMap.remove(key);
                    refreshRouteMapping();
                    logger.info("删除 路由配置映射关系 : {}", ROUTE_PREFIX + key);
                    return;
                }

                routeEntityMap.put(key, parseMapping(newValue));
                refreshRouteMapping();
                logger.info("修改 路由配置映射关系 : {}", ROUTE_PREFIX + key);
            }
        });
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
            e.printStackTrace();
        }
        return routeEntity;
    }

    /**
     * 通过spring config 刷新spring bean
     */
    private void refreshRouteMapping(){
        refreshScope.refresh(DYNAMIC_ROUTE_BEAN);
    }
}
