package com.aha.tech.core.event;

import com.aha.tech.core.service.RuntimeConfigChangedService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private RuntimeConfigChangedService runtimeConfigChangedService;


    /**
     * apollo 配置变更
     *
     * @param changeEvent
     */
    @ApolloConfigChangeListener({"application"})
    private void configChangeListener(ConfigChangeEvent changeEvent) {
        logger.info("配置中心 配置被发布!");
        Set<String> changeKeys = changeEvent.changedKeys();

        runtimeConfigChangedService.routeApiUriChanged(changeEvent,changeKeys);
        runtimeConfigChangedService.routeApiWhiteListChanged(changeEvent,changeKeys);
    }

}
