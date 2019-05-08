package com.aha.tech.config;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/3/13
 *
 * 网关白名单
 */
@Configuration
@ConfigurationProperties(prefix = "route.api.white")
public class WhiteListConfiguration {

    private List<String> list = new ArrayList<>();

    @Bean("whiteList")
    public List<String> whiteList() {
        return list;
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
