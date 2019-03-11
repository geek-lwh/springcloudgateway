//package com.aha.tech.config;
//
//import com.aha.tech.commons.symbol.Separator;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.collect.Maps;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * @Author: luweihong
// * @Date: 2019/3/5
// */
//@Configuration
//@ConfigurationProperties(prefix = "route.skip.authorization")
//public class SkipAuthorizationMappingsConfiguration {
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private Map<String, String> mappings = new HashMap<>();
//
//    @Bean
//    public Map<String, List<String>> skipAuthorizationMappings() {
//        Map<String, List<String>> skipAuthorizationMap = Maps.newHashMap();
//        for (Map.Entry<String, String> entry : mappings.entrySet()) {
//            String k = entry.getKey();
//            String v = entry.getValue();
//            List<String> list = Arrays.asList(StringUtils.split(v,Separator.COMMA_MARK));
//            skipAuthorizationMap.put(k,list);
//        }
//
//        return skipAuthorizationMap;
//    }
//
//    public Map<String, String> getMappings() {
//        return mappings;
//    }
//
//    public void setMappings(Map<String, String> mappings) {
//        this.mappings = mappings;
//    }
//
//}
