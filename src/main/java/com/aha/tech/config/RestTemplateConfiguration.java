package com.aha.tech.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author: monkey
 * @Date: 2018/7/29
 */
@Configuration
public class RestTemplateConfiguration {

    @Resource
    private CloseableHttpClient defaultCloseableHttpClient;

    @Resource
    private MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Resource
    private Runnable idleConnectionMonitor;

    /**
     * restTemplate 使用jackson做httpMessageConverter
     * 拦截器打印request和response的信息
     * @return
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(Collections.singletonList(mappingJackson2HttpMessageConverter));
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(clientHttpRequestFactory()));

        return restTemplate;
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(defaultCloseableHttpClient);
        return clientHttpRequestFactory;
    }

    /**
     * 定期清理 http pool idle resource
     */
    @PostConstruct
    public void cleanHttpPoolIdleResourceScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleWithFixedDelay(idleConnectionMonitor, 1, 10, TimeUnit.SECONDS);
    }

}
