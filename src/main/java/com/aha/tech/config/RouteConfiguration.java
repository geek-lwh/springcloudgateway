package com.aha.tech.config;

import com.aha.tech.core.model.entity.RouteEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: luweihong
 * @Date: 2019/3/5
 * 网关路由配置
 */
@Component
public class RouteConfiguration implements RouteDefinitionLocator {

    private static final Logger logger = LoggerFactory.getLogger(RouteConfiguration.class);

    private Set<RouteDefinition> runtimeRouteCache = new HashSet<>();

    public static volatile AtomicBoolean routeChange = new AtomicBoolean(true);

    @Resource
    private Map<String, RouteEntity> routeEntityMap;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        if (routeChange.get() == true) {
            logger.info("刷新运行时路由配置");
            Set<RouteDefinition> routeDefinitionSet = refreshRuntimeRoute();
            runtimeRouteCache.addAll(routeDefinitionSet);
            routeChange.compareAndSet(true, false);
        }

        logger.info("运行时路由配置 : {}", runtimeRouteCache);
        return Flux.fromIterable(runtimeRouteCache);
    }

    /**
     * 刷新运行时路由
     * @return
     */
    private Set<RouteDefinition> refreshRuntimeRoute() {
        Set<RouteDefinition> routeDefinitionSet = new HashSet<>();
        routeEntityMap.forEach((id, routeEntity) -> {
            RouteDefinition routeDefinition = configRouteDefinition(id,routeEntity);
            // hystrix properties
//            String groupKey = "hystrix_group_" + id;
//            HystrixObservableCommand.Setter setter = HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
//                    .andCommandKey(HystrixCommandKey.Factory.asKey(id));
//
//            HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
//            commandProperties.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE);
//            commandProperties.withExecutionTimeoutInMilliseconds(10_000);
//
//            setter.andCommandPropertiesDefaults(commandProperties);
//            filter1Params.put("setter",config);
//            filter1Params.put("fallbackUri", "addParam");


//// 名称是固定的，spring gateway会根据名称找对应的FilterFactory
//            filterDefinition.setName("RequestRateLimiter");
//// 每秒最大访问次数
//            filterParams.put("redis-rate-limiter.replenishRate", "2");
//// 令牌桶最大容量
//            filterParams.put("redis-rate-limiter.burstCapacity", "3");
//// 限流策略(#{@BeanName})
//            filterParams.put("key-resolver", "#{@hostAddressKeyResolver}");
//// 自定义限流器(#{@BeanName})

            routeDefinitionSet.add(routeDefinition);
        });

        return routeDefinitionSet;
    }

    /**
     * 配置路由
     * @param id
     * @param routeEntity
     * @return
     */
    private RouteDefinition configRouteDefinition(String id,RouteEntity routeEntity) {
        RouteDefinition routeDefinition = new RouteDefinition();
        // 配置route
        routeDefinition.setId(id);

        // 配置route
        PredicateDefinition predicate = predicateDefinition(routeEntity.getPath());
        routeDefinition.getPredicates().add(predicate);

        // uri
        URI uri = URI.create(routeEntity.getUri());
        routeDefinition.setUri(uri);

        // filter
        FilterDefinition filter1 = filterDefinition(id);
//        routeDefinition.setFilters(Arrays.asList(filter1));

        return routeDefinition;
    }

    /**
     * 配置路由的匹配模式和匹配表达式
     * @param pattern
     * @return
     */
    private PredicateDefinition predicateDefinition (String pattern){
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Path");
        Map<String, String> predicateParams = new HashMap<>(8);
        predicateParams.put("pattern", pattern);
        predicate.setArgs(predicateParams);

        return predicate;
    }

    /**
     * 配置路由的普通过滤器
     * @param name
     * @return
     */
    private FilterDefinition filterDefinition(String name){
        FilterDefinition filter1 = new FilterDefinition();
        filter1.setName("Hystrix");
        Map<String, String> filter1Params = new HashMap<>(8);
        filter1Params.put("name", name);
        filter1Params.put("fallbackUri", "forward:/fallback");
        filter1.setArgs(filter1Params);

        return filter1;
    }

}
