package com.aha.tech.config;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * @Author: monkey
 * @Date: 2018/7/28
 */
@Configuration
public class HttpClientConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfiguration.class);

    // 链接目标URI超时时间.
    private static final int CONNECT_TIMEOUT = 5000;

    // 从http pool中获取资源的超时时间.
    private static final int REQUEST_TIMEOUT = 5000;

    // 流读取超时时间
    private static final int SOCKET_TIMEOUT = 10000;

    // 最大 http pool链接资源数,根据具体的业务自己定义
    private static final int MAX_TOTAL_CONNECTIONS = 100;

    // keep alive保持长连接不断开的时间
    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS = 5 * 1000;

    // 关闭空闲http资源的时间
    private static final int CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS = 30;

    /**
     * http池化管理,支持https
     * @return
     */
    @Bean
    public PoolingHttpClientConnectionManager poolingConnectionManager() {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("http连接池 初始化发生异常,原因是: " + e.getMessage(), e);
        }

        SSLConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(builder.build());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            LOGGER.error("http连接池 初始化发生异常,原因是: " + e.getMessage(), e);
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create().register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();

        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingConnectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS); // 100

        // 限制每个host最大并发请求数,这里 max = x,则具体某个host为 x/2,客户端根据业务自己配置
        poolingConnectionManager.setDefaultMaxPerRoute(MAX_TOTAL_CONNECTIONS / 2); // 50 host max www.baidu.com
        return poolingConnectionManager;
    }

    /**
     * 长连接策略的具体配置
     * 会根据response判断是否带有长连接的标识符,如果有则查看是否带有timeout
     * 如果有timeout则以response的值为准,如果没有timeout则取默认值
     * @return
     */
    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();

                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return DEFAULT_KEEP_ALIVE_TIME_MILLIS;
        };
    }

    /**
     * 系统初始化默认的httpclient
     * @return
     */
    @Bean
    public CloseableHttpClient defaultCloseableHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(REQUEST_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT).build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnectionManager())
                .setKeepAliveStrategy(connectionKeepAliveStrategy())
                .build();
    }


    /**
     * 10秒一次定时清理闲置的链接和关闭过期的链接
     * @param connectionManager
     * @return
     */
    @Bean
    public Runnable idleConnectionMonitor(final PoolingHttpClientConnectionManager connectionManager) {
        return () -> {
            try {
                if (connectionManager != null) {
                    LOGGER.trace("开始清理闲置的链接和关闭过期的链接");
                    connectionManager.closeExpiredConnections();
                    connectionManager.closeIdleConnections(CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS, TimeUnit.SECONDS);
                } else {
                    LOGGER.warn("初始化http连接池监控线程 不成功");
                }
            } catch (Exception e) {
                LOGGER.error("http连接池监控线程 出现异常. msg={}, e={}", e.getMessage(), e);
            }
        };
    }

}
