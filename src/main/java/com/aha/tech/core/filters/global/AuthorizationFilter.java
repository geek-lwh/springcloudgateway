package com.aha.tech.core.filters.global;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.core.constant.FilterProcessOrderedConstant;
import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.LogUtil;
import com.aha.tech.util.TracerUtil;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.AttributeConstant.USER_ID;

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

    @Resource
    private Tracer tracer;

    @Override
    public int getOrder() {
        return FilterProcessOrderedConstant.AUTH_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = TracerUtil.startAndRef(exchange, this.getClass().getSimpleName());
        LogUtil.combineTraceId(exchange);
        ExchangeSupport.setActiveSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtil.setClue(span, exchange);
            ResponseVo responseVo = verifyAccessToken(exchange);
            Integer code = responseVo.getCode();
            RequestAddParamsDto requestAddParamsDto = ExchangeSupport.getRequestAddParamsDto(exchange);
            span.setTag(USER_ID, requestAddParamsDto.getUserId());
            if (!code.equals(ResponseConstants.SUCCESS)) {
                ExchangeSupport.setHttpStatus(exchange, HttpStatus.UNAUTHORIZED);
                Tags.ERROR.set(span, true);
                ExchangeSupport.fillErrorMsg(exchange, "Token 校验失败");
                return Mono.defer(() -> ResponseSupport.interrupt(exchange, HttpStatus.UNAUTHORIZED, responseVo));
            }

            return chain.filter(exchange);
        } catch (Exception e) {
            TracerUtil.logError(e);
            throw e;
        } finally {
            span.finish();
        }

    }

    /**
     * 是否通过授权
     * @param exchange
     * @return
     */
    private ResponseVo verifyAccessToken(ServerWebExchange exchange) {
        AuthenticationResultEntity authenticationResultEntity = httpRequestHandlerService.authorize(exchange);
        Boolean isWhiteList = authenticationResultEntity.getWhiteList();
        Integer code = authenticationResultEntity.getCode();
        if (isWhiteList || code.equals(ResponseConstants.SUCCESS)) {
            return new ResponseVo(ResponseConstants.SUCCESS);
        }

        String message = authenticationResultEntity.getMessage();
        logger.warn("授权异常 : {}", message);

        return new ResponseVo(code, message);
    }

}
