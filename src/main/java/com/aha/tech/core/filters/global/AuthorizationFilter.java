package com.aha.tech.core.filters.global;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.core.constant.FilterProcessOrderedConstant;
import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.LogUtils;
import com.aha.tech.util.TracerUtils;
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

import static com.aha.tech.core.constant.AttributeConstant.HTTP_STATUS;
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
        Span span = TracerUtils.startAndRef(exchange, this.getClass().getName());
        ExchangeSupport.setSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span, exchange);
            ResponseVo responseVo = verifyAccessToken(exchange);
            Integer code = responseVo.getCode();
            RequestAddParamsDto requestAddParamsDto = ExchangeSupport.getRequestAddParamsDto(exchange);
            span.setTag(USER_ID, requestAddParamsDto.getUserId());
            if (!code.equals(ResponseConstants.SUCCESS)) {
                ExchangeSupport.setHttpStatus(exchange, HttpStatus.UNAUTHORIZED);
                span.setTag(HTTP_STATUS, HttpStatus.UNAUTHORIZED.value());
                span.log(responseVo.getMessage());
                Tags.ERROR.set(span, true);
                return Mono.defer(() -> ResponseSupport.write(exchange, HttpStatus.UNAUTHORIZED, responseVo));
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
     * 是否通过授权
     * @param exchange
     * @return
     */
    private ResponseVo verifyAccessToken(ServerWebExchange exchange) {
        LogUtils.combineLog(exchange);

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
