package com.aha.tech.config;

import com.aha.tech.serializer.FastJsonSerializer;
import com.alibaba.fastjson.parser.ParserConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * redis配置
 *
 * @Author: luweihong
 * @Date: 2018/7/25
 */
@Configuration
public class RedisConfiguration {

    static final Logger logger = LoggerFactory.getLogger(RedisConfiguration.class);

    @Value("${redis.host:localhost}")
    public String redisHost;

    @Value("${redis.port:6379}")
    public Integer redisPort;

    @Value("${redis.password}")
    public String redisPassword;

    @Value("${redis.timeout:10000}")
    public Integer redisTimeout;

    @Value("${redis.database:0}")
    public Integer redisDatabase;

    @Value("${redis.max-active:50}")
    public Integer redisMaxActive;

    @Value("${redis.max-wait:10}")
    public Integer redisMaxWait;

    @Value("${redis.max-idle:15}")
    public Integer redisMaxIdle;

    @Value("${redis.min-idle:5}")
    public Integer redisMinIdle;

    /**
     * redis factory
     */
    @Bean("redisConnectionFactory")
    public LettuceConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfig = lettuceClientConfiguration(redisMaxActive, redisMaxIdle, redisMinIdle, redisMaxWait);
        RedisStandaloneConfiguration redisStandaloneConfiguration = redisStandaloneConfiguration(redisHost, redisPort, redisPassword, redisDatabase);
        return createLettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }

    /**
     * 构建lettuceClientConfiguration
     *
     * @param maxActive 最大活跃数
     * @param maxIdle   最大空闲数
     * @param minIdle   最小空闲数
     * @param maxWait   最大等待时间
     */
    LettuceClientConfiguration lettuceClientConfiguration(Integer maxActive, Integer maxIdle, Integer minIdle, Integer maxWait) {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(maxActive);
        config.setMinIdle(minIdle);
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWait);
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder()
                .poolConfig(config).commandTimeout(Duration.ofSeconds(redisTimeout));
        LettuceClientConfiguration clientConfig = builder.build();
        return clientConfig;
    }

    /**
     * 构建redis config
     *
     * @param host     redis服务端地址
     * @param port     redis服务端端口号
     * @param password redis 密码
     * @param database redis 实例编号
     */
    RedisStandaloneConfiguration redisStandaloneConfiguration(String host, Integer port, String password, Integer database) {
        logger.info(String.format("初始化公用redis ----> url : %s:%s,使用database [%s],password=%s", host, port, database, password));
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setPassword(RedisPassword.of(password));
        config.setDatabase(database);
        return config;
    }


    /**
     * 构建redis工厂
     */
    LettuceConnectionFactory createLettuceConnectionFactory(RedisStandaloneConfiguration redisStandaloneConfiguration, LettuceClientConfiguration clientConfiguration) {
        //if (getSentinelConfig() != null) {
        //    return new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
        //}
        //if (getClusterConfiguration() != null) {
        //    return new LettuceConnectionFactory(getClusterConfiguration(),
        //            clientConfiguration);
        //}
        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfiguration);
    }

    /**
     * 自定义redis序列化 使用fast json
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> privateRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        FastJsonSerializer<Object> fastJsonSerializer = new FastJsonSerializer<>(Object.class);
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(fastJsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(fastJsonSerializer);
        template.setEnableTransactionSupport(Boolean.TRUE);
        template.setConnectionFactory(redisConnectionFactory());
        template.afterPropertiesSet();
        return template;
    }

}
