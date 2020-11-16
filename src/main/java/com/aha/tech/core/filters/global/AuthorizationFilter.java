package com.aha.tech.core.filters.global;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.core.constant.FilterProcessOrderedConstant;
import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.TRACE_LOG_ID;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 *
 * 鉴权校验
 */
@Component
public class AuthorizationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return FilterProcessOrderedConstant.AUTH_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = GlobalTracer.get().buildSpan(this.getClass().getName());
        Span parentSpan = ExchangeSupport.getSpan(exchange);
        Span span = spanBuilder.asChildOf(parentSpan).start();
        ExchangeSupport.setSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span);
            ExchangeSupport.put(exchange, TRACE_LOG_ID, span.context().toTraceId());
            return isPassed(exchange, chain);
        } catch (Exception e) {
            TracerUtils.reportErrorTrace(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    /**
     * 是否通过授权
     * @param exchange
     * @param chain
     * @return
     */
    private Mono<Void> isPassed(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getAttributeOrDefault(TRACE_LOG_ID, "MISS_TRACE_ID");
        MDC.put("traceId", traceId);

        AuthenticationResultEntity authenticationResultEntity = httpRequestHandlerService.authorize(exchange);
        Boolean isWhiteList = authenticationResultEntity.getWhiteList();
        if (isWhiteList) {
            return chain.filter(exchange);
        }

        Integer code = authenticationResultEntity.getCode();
        if (code.equals(ResponseConstants.SUCCESS)) {
            return chain.filter(exchange);
        }

        String message = authenticationResultEntity.getMessage();
        logger.warn("授权异常 : {}", message);
        return Mono.defer(() -> {
            ResponseVo rpcResponse = new ResponseVo(code, message);
            return ResponseSupport.write(exchange, HttpStatus.UNAUTHORIZED, rpcResponse);
        });
    }

}
