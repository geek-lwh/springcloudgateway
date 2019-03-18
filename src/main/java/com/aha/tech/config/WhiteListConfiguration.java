package com.aha.tech.config;

import com.aha.tech.commons.symbol.Separator;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: luweihong
 * @Date: 2019/3/13
 *
 * 网关白名单
 */
@Configuration
@ConfigurationProperties(prefix = "route.api.whitelist")
public class WhiteListConfiguration {

    private Map<String, String> mappings = new HashMap<>();

    @Bean("whiteListMap")
    public Map<String, List<String>> whiteListMap() {
        Map<String, List<String>> whiteListMap = Maps.newHashMap();

        mappings.forEach((k, v) -> {
            String[] whiteListArray = StringUtils.split(v, Separator.COMMA_MARK);
            List<String> whiteList = Arrays.asList(whiteListArray);
            whiteListMap.put(k, whiteList);
        });

        return whiteListMap;
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, String> mappings) {
        this.mappings = mappings;
    }
}
