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
 */
@Configuration
@ConfigurationProperties(prefix = "route")
public class GatewayRouteMappingsConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, String> mappings = new HashMap<>();

    @Bean
    public Map<String, RouteEntity> routeEntityMap() {
        Map<String, RouteEntity> routeEntityMap = Maps.newConcurrentMap();
        mappings.forEach((k, v) -> {

            RouteEntity routeEntity = null;
            try {
                routeEntity = objectMapper.readValue(v, RouteEntity.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            routeEntityMap.put(k, routeEntity);
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
