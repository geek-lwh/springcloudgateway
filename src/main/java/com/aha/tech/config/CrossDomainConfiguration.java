package com.aha.tech.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/4/10
 */
@Configuration
public class CrossDomainConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(ALL_CONTROL_ALLOW_ORIGIN_ACCESS_2);
        config.setAllowedMethods(CROSS_ACCESS_ALLOW_HTTP_METHODS_2);
        config.setAllowedHeaders(CROSS_ACCESS_ALLOW_ALLOW_HEADERS);
        config.setMaxAge(CROSS_ACCESS_ALLOW_MAX_AGE);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

}
