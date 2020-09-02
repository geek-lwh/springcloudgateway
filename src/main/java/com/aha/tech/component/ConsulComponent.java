package com.aha.tech.component;

import com.aha.tech.commons.utils.IpUtil;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author: luweihong
 * @Date: 2020/9/2
 */
@Component
public class ConsulComponent implements CommandLineRunner {

    private Logger logger = LoggerFactory.getLogger(ConsulComponent.class);

    private final static String CONTEXT_PATH_META_DATA = "context_path";

    private final static String PROTOCOL_PREFIX = "http://";

    private final static String API_SUFFIX = "/actuator/prometheus";

    @Value("${common.consul.address.host:10.10.129.240}")
    private String consulAddressHost;

    @Value("${common.consul.address.port:8500}")
    private int consulAddressPort;

    @Value("${common.consul.server.name:${spring.application.name}}")
    private String serverName;

    @Value("${common.consul.server.port:${server.port}}")
    private int serverPort;

    @Value("${common.consul.check.interval:10s}")
    private String checkInterval;

    @Value("${common.server.tomcat.contextPath:/}")
    private String contextPath;


    private void registerForPrometheus() {
        try {
            ConsulClient client = new ConsulClient(consulAddressHost, consulAddressPort);
            String ip = IpUtil.getLocalHostAddress();
            String id = serverName + "_" + ip;
            NewService newService = new NewService();
            newService.setId(id);
            newService.setName(serverName);
            newService.setPort(serverPort);
            newService.setAddress(ip);

            String suffix = contextPath.equals("/") ? API_SUFFIX : contextPath + API_SUFFIX;
            String checkUrl = PROTOCOL_PREFIX + ip + ":" + serverPort + suffix;

            Map<String, String> properties = Maps.newHashMap();
            properties.put(CONTEXT_PATH_META_DATA, suffix);
            newService.setMeta(properties);

            NewService.Check serviceCheck = new NewService.Check();
            serviceCheck.setHttp(checkUrl);
            serviceCheck.setInterval(checkInterval);
            newService.setCheck(serviceCheck);

            client.agentServiceRegister(newService);
        } catch (Exception e) {
            logger.error("注册consul时,计算ip异常", e);
        }
    }

    @Override
    public void run(String... args) {
        registerForPrometheus();
    }
}
