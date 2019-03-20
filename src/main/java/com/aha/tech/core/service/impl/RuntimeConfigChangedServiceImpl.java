package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.config.IpRateLimiterConfiguration;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: luweihong
 * @Date: 2019/3/18
 */
@Service("runtimeConfigChangedService")
public class RuntimeConfigChangedServiceImpl implements RuntimeConfigChangedService {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigChangedServiceImpl.class);

    private String DYNAMIC_ROUTE_API_URI_BEAN = "routeEntityMap";

    private String DYNAMIC_ROUTE_API_WHITE_LIST_BEAN = "whiteListMap";

    private String IP_RATE_LIMITER_CONFIGURATION_BEAN = "ipRateLimiterConfiguration";

    private String ROUTE_API_URI_PREFIX = "route.api.uri.mappings.";

    private String ROUTE_API_WHITE_LIST_PREFIX = "route.api.whitelist.mappings.";

    private String IP_RATELIMITER_DEFAULT_REPLENISH_RATE = "ip.ratelimiter.default.replenish.rate";

    private String IP_RATELIMITER_DEFAULT_BURST_CAPACITY = "ip.ratelimiter.default.burst.capacity";

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
    public void routeApiUriChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys) {
        changeKeys.forEach(changeKeyName -> {
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.startsWith(ROUTE_API_URI_PREFIX)) {
                refreshRouteApiUriConfig(change, ROUTE_API_URI_PREFIX, changeKeyName);
                for (; ; ) {
                    if (RouteConfiguration.needToSyncLocal.compareAndSet(false, true)) {
                        break;
                    }
                }
            }

        });

    }

    /**
     * 路由API白名单列表配置变更
     * @param changeEvent
     * @param changeKeys
     */
    @Override
    public void routeApiWhiteListChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys) {
        changeKeys.forEach(changeKeyName -> {
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.startsWith(ROUTE_API_WHITE_LIST_PREFIX)) {
                refreshRouteApiWhiteListConfig(change,ROUTE_API_WHITE_LIST_PREFIX,changeKeyName);
            }
        });
    }

    @Override
    public void ipRateLimiterChanged(ConfigChangeEvent changeEvent, Set<String> changeKeys) {
        changeKeys.forEach(changeKeyName -> {
            ConfigChange change = changeEvent.getChange(changeKeyName);
            if (changeKeyName.equals(IP_RATELIMITER_DEFAULT_REPLENISH_RATE)) {
                refreshIpReplenishRate(change,changeKeyName);
            }
            if (changeKeyName.equals(IP_RATELIMITER_DEFAULT_BURST_CAPACITY)) {
                refreshIpBurstCapacityRate(change,changeKeyName);
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
     * 路由API白名单列表变更
     * @param change
     * @param prefix
     * @param changeKeyName
     */
    private void refreshRouteApiWhiteListConfig(ConfigChange change, String prefix, String changeKeyName) {
        logger.info("路由API白名单列表变更 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        Map<String, List<String>> whiteListMap = (Map<String, List<String>>) SpringContextUtil.getBean(DYNAMIC_ROUTE_API_WHITE_LIST_BEAN);

        String key = changeKeyName.substring(prefix.length());

        if (StringUtils.isBlank(newValue)) {
            whiteListMap.remove(key);
            refreshScope.refresh(DYNAMIC_ROUTE_API_WHITE_LIST_BEAN);
            logger.info("删除 路由API白名单列表 : {}", prefix + key);
            return;
        }

        List<String> list = Arrays.asList(StringUtils.split(newValue, Separator.COMMA_MARK));
        whiteListMap.put(key, list);
        refreshScope.refresh(DYNAMIC_ROUTE_API_WHITE_LIST_BEAN);
        logger.info("更新 路由API白名单列表 : {}", prefix + key);
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
     * 刷新ip速率
     * @param change
     */
    private void refreshIpReplenishRate(ConfigChange change,String beanName) {
        logger.info("ip速率变更 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        IpRateLimiterConfiguration configuration = (IpRateLimiterConfiguration) SpringContextUtil.getBean(IP_RATE_LIMITER_CONFIGURATION_BEAN);
        Integer ipReplenishRate = Integer.parseInt(newValue);

        if(ipReplenishRate.compareTo(1) == -1){
            logger.error("速率变更必须大于 1,当前值 : {}",ipReplenishRate);
            throw new IllegalArgumentException();
        }
        configuration.setReplenishRate(ipReplenishRate);
        refreshScope.refresh(beanName);
        logger.info("更新 ip速率的值为 : {}", ipReplenishRate);
    }

    /**
     * 更新ip容量
     * @param change
     */
    private void refreshIpBurstCapacityRate(ConfigChange change,String beanName) {
        logger.info("ip容量变更 !");
        String oldValue = change.getOldValue();
        String newValue = change.getNewValue();
        logger.info("变更前的值 : {},变更后的值 : {}", oldValue, newValue);

        IpRateLimiterConfiguration configuration = (IpRateLimiterConfiguration) SpringContextUtil.getBean(IP_RATE_LIMITER_CONFIGURATION_BEAN);
        Integer burstCapacity = Integer.parseInt(newValue);

        if(burstCapacity.compareTo(1) == -1){
            logger.error("速率变更必须大于 1,当前值 : {}",burstCapacity);
            throw new IllegalArgumentException();
        }
        configuration.setBurstCapacity(burstCapacity);
        refreshScope.refresh(beanName);
        logger.info("更新 ip容量的值为 : {}", burstCapacity);
    }

}
