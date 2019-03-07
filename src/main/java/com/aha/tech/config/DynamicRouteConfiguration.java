package com.aha.tech.config;

import com.aha.tech.core.model.entity.RouteEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author: luweihong
 * @Date: 2019/3/5
 */
@Component
public class DynamicRouteConfiguration implements RouteDefinitionLocator {

    private static final Logger logger = LoggerFactory.getLogger(DynamicRouteConfiguration.class);

    @Resource
    private Map<String, RouteEntity> routeEntityMap;

    @Autowired(required = false)
    private RouteDefinition routeDefinition;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        Set<RouteDefinition> routeDefinitionList = new HashSet<>();
        routeEntityMap.forEach((id, routeEntity) -> {
            RouteDefinition routeDefinition = new RouteDefinition();

            // 自定义Predicate
            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setName("Path");
            Map<String, String> predicateParams = new HashMap<>(8);
            predicateParams.put("pattern", routeEntity.getPath());
            predicate.setArgs(predicateParams);

            // uri
            URI uri = URI.create(routeEntity.getUri());

            routeDefinition.setId(routeEntity.getId());
            routeDefinition.setUri(uri);
            routeDefinition.getPredicates().add(predicate);

            routeDefinitionList.add(routeDefinition);
        });

        logger.info("mappings is : {}", routeDefinitionList);
        return Flux.fromIterable(routeDefinitionList);
    }

}
