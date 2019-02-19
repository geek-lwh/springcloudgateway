package com.aha.tech.core.routes;

import com.aha.tech.core.filters.AuthGatewayFilterFactory;
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
                        .path("/aha-account/**")
//                        .filters(f -> f.addRequestParameter("user_id","123123123"))
                        .uri("lb://accountserver")

                        .id("auth"))
                .build();
    }

}
