//package com.aha.tech.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.codec.ServerCodecConfigurer;
//import org.springframework.http.codec.json.Jackson2JsonDecoder;
//import org.springframework.http.codec.json.Jackson2JsonEncoder;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.web.reactive.config.EnableWebFlux;
//import org.springframework.web.reactive.config.WebFluxConfigurer;
//
///**
// * @Author: luweihong
// * @Date: 2018/7/26
// * 驼峰转下划线 网关暂时不适用
// * 也不推荐使用@EnableMVC等标签让项目编程web应用
// * 使用实现webmvc
// */
//@Configuration
//public class WebConfiguration implements WebFluxConfigurer {
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @Bean
//    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
//        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
//        return jsonConverter;
//    }
//
//    /**
//     * webflux http message
//     * @param serverCodecConfigurer
//     */
//    @Override
//    public void configureHttpMessageCodecs(ServerCodecConfigurer serverCodecConfigurer) {
//        Jackson2JsonEncoder jackson2JsonEncoder = new Jackson2JsonEncoder(objectMapper);
//        jackson2JsonEncoder.setStreamingMediaTypes(mappingJackson2HttpMessageConverter().getSupportedMediaTypes());
//
//        Jackson2JsonDecoder jackson2JsonDecoder = new Jackson2JsonDecoder(objectMapper);
//        serverCodecConfigurer.defaultCodecs().jackson2JsonEncoder(jackson2JsonEncoder);
//        serverCodecConfigurer.defaultCodecs().jackson2JsonDecoder(jackson2JsonDecoder);
//    }
//}
