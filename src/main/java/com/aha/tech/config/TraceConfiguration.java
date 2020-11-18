package com.aha.tech.config;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.metrics.NoopMetricsFactory;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: luweihong
 * @Date: 2020/11/4
 *
 * jaeger配置
 * 定义reporter,sender,simpler等策略
 *
 */
@Configuration
public class TraceConfiguration {

    private final Logger logger = LoggerFactory.getLogger(TraceConfiguration.class);

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${jaeger.agent.host:localhost}")
    private String agentHost;

    @Value("${jaeger.agent.port:6831}")
    private Integer agentPort;

    @Value("${jaeger.sampler.enable:true}")
    private Boolean enable;

    @Value("${jaeger.reporter.flush.interval:500}")
    private Integer flushInterval;

    @Value("${jaeger.reporter.max.queue.size:1000}")
    private Integer maxQueueSize;

    @Bean
    public JaegerTracer tracer() {
        io.jaegertracing.Configuration.SenderConfiguration sender = new io.jaegertracing.Configuration.SenderConfiguration();
        sender.withAgentHost(agentHost).withAgentPort(agentPort);

        io.jaegertracing.Configuration.ReporterConfiguration reporterConfiguration = new io.jaegertracing.Configuration.ReporterConfiguration();
        reporterConfiguration.withSender(sender).withFlushInterval(flushInterval).withMaxQueueSize(maxQueueSize).withLogSpans(Boolean.TRUE);

        io.jaegertracing.Configuration.SamplerConfiguration samplerConfiguration = new io.jaegertracing.Configuration.SamplerConfiguration();
        Integer param = enable.equals(Boolean.TRUE) ? 1 : 0;
        samplerConfiguration.withType(ConstSampler.TYPE).withParam(param);

        io.jaegertracing.Configuration config = new io.jaegertracing.Configuration(serviceName);
        config.withReporter(reporterConfiguration)
                .withSampler(samplerConfiguration)
                .withServiceName(serviceName)
                .withMetricsFactory(new NoopMetricsFactory());


        JaegerTracer tracer = config.getTracer();
        GlobalTracer.registerIfAbsent(config.getTracer());

        return tracer;
    }

}
