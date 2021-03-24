package com.aha.tech.core.filters.global;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.core.constant.FilterProcessOrderedConstant;
import com.aha.tech.core.exception.AuthorizationFailedException;
import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.AttributeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.passportserver.facade.code.AuthorizationCode;
import com.aha.tech.util.TagsUtil;
import com.aha.tech.util.TraceUtil;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.apache.commons.lang3.StringUtils;
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

    private static final Integer FORCE_UPGRADE_CODE = 738002;

    private static final String FORCE_UPGRADE_MSG = "检测到当前孩子被删除，请升级到最新版本后正常使用！";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = TraceUtil.start(exchange, this.getClass().getSimpleName());
        try (Scope scope = tracer.scopeManager().activate(span)) {
            ResponseVo responseVo = verifyAccessToken(exchange);
            Integer code = responseVo.getCode();
            Boolean ignore = AttributeSupport.ignoreEmptyKidMapping(exchange);

            if (code.equals(ResponseConstants.SUCCESS)) {
                return chain.filter(exchange);
            }

            // 如果孩子账户被删除,但是在白名单中则忽略
            boolean emptyKid = emptyKidMapping(code);
            if (emptyKid && ignore) {
                return chain.filter(exchange);
            }

            // 如果孩子账户被删除,没有忽略,判断新老版本是否需要提示强制升级
            Boolean upgrade = AttributeSupport.shouldClientUpgrade(exchange);
            if (emptyKid && upgrade) {
                responseVo.setCode(FORCE_UPGRADE_CODE);
                responseVo.setMessage(FORCE_UPGRADE_MSG);
                span.log(FORCE_UPGRADE_MSG);
                return Mono.defer(() -> ResponseSupport.interrupt(exchange, HttpStatus.UNAUTHORIZED, responseVo));
            }

            AttributeSupport.setHttpStatus(exchange, HttpStatus.UNAUTHORIZED);
            span.log(responseVo.getMessage());
            Tags.ERROR.set(span, true);
            AttributeSupport.fillErrorMsg(exchange, responseVo.getMessage());
            return Mono.defer(() -> ResponseSupport.interrupt(exchange, HttpStatus.UNAUTHORIZED, responseVo));

        } catch (Exception e) {
            TagsUtil.setCapturedErrorsTags(e);
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
        Boolean isSkipAuth = authenticationResultEntity.getSkipAuth();
        Integer code = authenticationResultEntity.getCode();
        if (isSkipAuth || code.equals(ResponseConstants.SUCCESS)) {
            return new ResponseVo(ResponseConstants.SUCCESS);
        }

        String message = authenticationResultEntity.getMessage();

        if (StringUtils.isBlank(message)) {
            message = AuthorizationFailedException.AUTHORIZATION_FAILED_ERROR_MSG;
        }
        logger.warn("授权异常 : {}", message);

        return new ResponseVo(code, message);
    }

    /**
     * 返回码是不是5300
     * @param code
     * @return
     */
    private boolean emptyKidMapping(Integer code) {
        return code.equals(AuthorizationCode.WRONG_KID_ACCOUNT_CODE);
    }
}
