package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.AttributeConstant;
import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.controller.FallBackController;
import com.aha.tech.core.exception.LimiterException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.LimiterService;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.LogUtils;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Map;

import static com.aha.tech.core.constant.AttributeConstant.HTTP_STATUS;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.IP_RATE_LIMITER_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/3/20
 *
 * qps限流策略
 */
@Component
public class IpRateLimiterFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(IpRateLimiterFilter.class);

    public static final String IP_RATE_LIMITER_ERROR_MSG = "IP限流策略生效";

    @Resource
    private LimiterService ipLimiterService;

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Value("${ip.ratelimiter.enable:false}")
    private boolean isEnable;

    @Resource
    private Tracer tracer;

    @Override
    public int getOrder() {
        return IP_RATE_LIMITER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = TracerUtils.startAndRef(exchange, this.getClass().getName());
        LogUtils.combineLog(exchange);

        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span, exchange);
            Boolean isAllowed = isIpAllowed(exchange);
            String ip = exchange.getRequest().getHeaders().get(HeaderFieldConstant.HEADER_X_FORWARDED_FOR).get(0);
            span.setTag(AttributeConstant.IP_LIMITER_ATTR, ip);
            Map<String, String> info = ExchangeSupport.getCurrentIpLimiter(exchange);
            if (!isAllowed) {
                info.put(Fields.EVENT, Tags.ERROR.getKey());
                info.put(Fields.MESSAGE, IP_RATE_LIMITER_ERROR_MSG);
                Tags.ERROR.set(span, true);
                span.setTag(HTTP_STATUS, HttpStatus.TOO_MANY_REQUESTS.value());
                span.log(info);
                logger.error("ip : {} 限流算法生效", ip);
                final ResponseVo responseVo = new ResponseVo(HttpStatus.TOO_MANY_REQUESTS.value(), FallBackController.DEFAULT_SYSTEM_ERROR);
                return Mono.defer(() -> ResponseSupport.write(exchange, responseVo, HttpStatus.TOO_MANY_REQUESTS, new LimiterException(IP_RATE_LIMITER_ERROR_MSG)));
            }

            return chain.filter(exchange);
        } catch (Exception e) {
            TracerUtils.logError(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    /**
     * 获取ip限流结果
     * @param exchange
     * @return
     */
    private Boolean isIpAllowed(ServerWebExchange exchange) {
        if (!isEnable) {
            return Boolean.TRUE;
        }

        String rawPath = exchange.getRequest().getURI().getRawPath();
//        String ip = exchange.getRequest().getHeaders().get(HeaderFieldConstant.HEADER_X_FORWARDED_FOR).get(0);
        if (httpRequestHandlerService.isSkipIpLimiter(rawPath)) {
            logger.info("跳过ip限流策略 : {}", rawPath);
            return Boolean.TRUE;
        }

        Boolean isAllowed = ipLimiterService.isAllowed(exchange);

        return isAllowed;
    }

}
