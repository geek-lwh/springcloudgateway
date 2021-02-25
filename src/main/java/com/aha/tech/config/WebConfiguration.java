package com.aha.tech.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import javax.annotation.Resource;

/**
 * @Author: luweihong
 * @Date: 2018/7/26
 * 驼峰转下划线 网关暂时不适用
 * 也不推荐使用@EnableMVC等标签让项目编程web应用
 * 使用实现webmvc
 */
@Configuration
public class WebConfiguration implements WebFluxConfigurer {

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        return jsonConverter;
    }

}
