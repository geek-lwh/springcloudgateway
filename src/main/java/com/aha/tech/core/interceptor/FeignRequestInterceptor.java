package com.aha.tech.core.interceptor;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.model.wrapper.FeignCarrierWrapper;
import com.aha.tech.util.TagsUtil;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;


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
        tracing(requestTemplate);
        feignRequestLogging(requestTemplate);
    }

    /**
     * 构建tracing
     * @param requestTemplate
     * @return
     */
    private void tracing(RequestTemplate requestTemplate) {
        Tracer tracer = GlobalTracer.get();
        if (tracer == null) return;
        Span span = tracer.activeSpan();
        if (span == null) return;

        try (Scope scope = tracer.scopeManager().activate(span)) {
            SpanContext spanContext = span.context();
            addRequestChainInfo(requestTemplate, spanContext);
            TagsUtil.setRpcTags(span, Tags.SPAN_KIND_CLIENT);
            tracer.inject(spanContext, Format.Builtin.HTTP_HEADERS, new FeignCarrierWrapper(requestTemplate));
        } catch (Exception e) {
            TagsUtil.setCapturedErrorsTags(e, span);
            logger.error(e.getMessage(), e);
        } finally {
            span.finish();
        }

    }

    /**
     * 添加头信息
     * @param requestTemplate
     * @param spanContext
     * @throws Exception
     */
    private void addRequestChainInfo(RequestTemplate requestTemplate, SpanContext spanContext) {
        requestTemplate.header(HeaderFieldConstant.TRACE_ID, spanContext.toTraceId());
        requestTemplate.header(HeaderFieldConstant.SPAN_ID, spanContext.toSpanId());
    }

    /**
     * feign调用日志
     * @param requestTemplate
     */
    private void feignRequestLogging(RequestTemplate requestTemplate) {
        if (!feignLog) return;
        StringBuilder sb = new StringBuilder(System.lineSeparator());
        sb.append("Feign request URI : ").append(requestTemplate.url()).append(requestTemplate.queryLine()).append(System.lineSeparator());
        sb.append("Feign request HEADER : ").append(requestTemplate.headers().toString()).append(System.lineSeparator());
        String body = requestTemplate.body() == null ? Strings.EMPTY : new String(requestTemplate.body(), Charset.forName("utf-8"));
        sb.append("Feign request BODY : ").append(body);

        logger.info("Feign request INFO : {}", sb);
    }

}
