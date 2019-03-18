package com.aha.tech.config;

import com.aha.tech.core.model.entity.RouteEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: luweihong
 * @Date: 2019/3/5
 * 运行时当前的路由映射关系
 */
@Configuration
@ConfigurationProperties(prefix = "route.api.uri")
public class RuntimeRouteConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, String> mappings = new HashMap<>();

    @Bean("routeEntityMap")
    public Map<String, RouteEntity> routeEntityMap() {
        Map<String, RouteEntity> routeEntityMap = refreshRouteEntityMap();
        return routeEntityMap;
    }

    /**
     * 刷新路由映射的实体映射对象
     * @return
     */
    public Map<String, RouteEntity> refreshRouteEntityMap(){
        Map<String, RouteEntity> routeEntityMap = Maps.newHashMapWithExpectedSize(mappings.size());
        mappings.forEach((k, v) -> {
            try {
                RouteEntity routeEntity = objectMapper.readValue(v, RouteEntity.class);
                routeEntityMap.put(k, routeEntity);

            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        return routeEntityMap;
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }

}
