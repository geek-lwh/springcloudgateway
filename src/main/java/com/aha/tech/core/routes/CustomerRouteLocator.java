package com.aha.tech.core.routes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: luweihong
 * @Date: 2019/2/13
 */
@Configuration
public class CustomerRouteLocator {


    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(p -> p
                        .path("/aha-account/**").and()
                        .uri("lb://accountserver")
//                        .filter(modifyResponseGatewayFilter)
                        .id("auth"))
                .build();
    }

}
