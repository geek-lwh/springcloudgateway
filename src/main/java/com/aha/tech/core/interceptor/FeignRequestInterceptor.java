package com.aha.tech.core.interceptor;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.model.wrapper.RequestBuilderCarrier;
import com.aha.tech.util.IpUtil;
import com.aha.tech.util.TracerUtils;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.util.GlobalTracer;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2018/12/3
 * <p>
 * feign request 拦截器
 * 用于动态传递header信息,如果有需要需要自己定义
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    private final Logger logger = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    private static Config config = ConfigService.getAppConfig();

    private static Boolean feignLog = config.getBooleanProperty("feign.log", Boolean.FALSE);

    private static String serverName = config.getProperty("spring.application.name", "UNKNOWN");

    private static int port = config.getIntProperty("common.server.tomcat.port", 0);

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Tracer tracer = GlobalTracer.get();
        if (tracer != null) {
            Span span = tracer.activeSpan();
            SpanContext spanContext = span.context();
            TracerUtils.setClue(span);
            requestTemplate.header(HeaderFieldConstant.TRACE_ID, spanContext.toTraceId());
            requestTemplate.header(HeaderFieldConstant.SPAN_ID, spanContext.toSpanId());
            requestTemplate.header(REQUEST_FROM, serverName);
            requestTemplate.header(REQUEST_API, requestTemplate.url());
            try {
                requestTemplate.header(REQUEST_ADDRESS, IpUtil.getLocalHostAddress() + ":" + port);
            } catch (Exception e) {
                logger.error("构建traceInfo时 计算ip地址出错", e);
            }
            tracer.inject(spanContext, Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestTemplate));
        }
        if (feignLog) {
            feignRequestLogging(requestTemplate);
        }
    }

    /**
     * feign调用日志
     * @param requestTemplate
     */
    private void feignRequestLogging(RequestTemplate requestTemplate) {
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        sb.append("Feign request URI : ").append(requestTemplate.url()).append(requestTemplate.queryLine()).append(System.lineSeparator());
        sb.append("Feign request HEADER : ").append(requestTemplate.headers().toString()).append(System.lineSeparator());
        String body = requestTemplate.body() == null ? Strings.EMPTY : new String(requestTemplate.body(), Charset.forName("utf-8"));
        sb.append("Feign request BODY : ").append(body);

        logger.info("Feign request INFO : {}", sb);
    }

}
