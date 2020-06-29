package com.aha.tech;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;


@EnableApolloConfig
@EnableWebFlux
@EnableFeignClients
@EnableHystrix
@EnableDiscoveryClient
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GatewayApplication {

    private static final Logger logger = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        String profile = System.getProperty("spring.profiles.active");
        Assert.notNull(profile, "请指定 [-Dspring.profiles.active]");
        SpringApplication app = new SpringApplication(GatewayApplication.class);
        try {
//            int nThreads = Runtime.getRuntime().availableProcessors();
//            String v = System.getProperty("reactor.netty.ioWorkerCount", String.valueOf(nThreads));
            System.setProperty("reactor.netty.ioWorkerCount", "24");
//            System.setProperty("com.alibaba.nacos.naming.log.level","info");
            ConfigurableApplicationContext configurableApplicationContext = app.run(args);
            Environment env = configurableApplicationContext.getEnvironment();
            validateProfiles(env, profile);
            printServerInfo(env);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> configurer(
            @Value("${spring.application.name}") String applicationName) {
        return (registry) -> registry.config().commonTags("application", applicationName);
    }

    /**
     * 校验-Dspring.profiles.active 的值是否合法
     * @param env
     */
    private static void validateProfiles(Environment env, String inputProfile) {
        Preconditions.checkNotNull(env.getActiveProfiles(), "请指定 [-Dspring.profiles.active]");
        List<String> profiles = Arrays.asList(env.getActiveProfiles());
        Preconditions.checkArgument(profiles.contains(inputProfile), "输入的-Dspring.profiles.active 与 有效的 active不服");
    }

    /**
     * 启动输出信息
     * @param env
     * @throws IOException
     */
    private static void printServerInfo(Environment env) throws IOException {
        String appBanner = StreamUtils.copyToString(new ClassPathResource("app-banner.txt").getInputStream(),
                Charset.defaultCharset());

        String applicationName = env.getProperty("spring.application.name");
        String servletContextPath = env.getProperty("server.servlet.context-path");
        String serverPort = env.getProperty("server.port");

        logger.info(appBanner, applicationName,
                StringUtils.isEmpty(env.getProperty("server.ssl.key-store")) ? "http" : "https",
                env.getProperty("server.port"), StringUtils.defaultString(servletContextPath),
                StringUtils.isEmpty(env.getProperty("server.ssl.key-store")) ? "http" : "https",
                InetAddress.getLocalHost().getHostAddress(), serverPort,
                StringUtils.defaultString(servletContextPath),
                org.springframework.util.StringUtils.arrayToCommaDelimitedString(env.getActiveProfiles()),
                env.getProperty("PID"), Charset.defaultCharset());
    }
}
