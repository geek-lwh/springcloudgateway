package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.OverwriteParamService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.WriteResponseSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.MODIFY_PARAMS_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 *
 * 修改GET,DELETE 等请求的参数
 */
@Component
public class ModifyRequestParamsFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestParamsFilter.class);

    @Resource
    private OverwriteParamService httpOverwriteParamService;

    private static final String USER_ID_FIELD = "user_id";

    @Override
    public int getOrder() {
        return MODIFY_PARAMS_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("开始进入修改GET|POST请求参数过滤器");

        Boolean isSkipAuth = ExchangeSupport.getIsSkipAuth(exchange);
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        MediaType mediaType = serverHttpRequest.getHeaders().getContentType();
        HttpMethod httpMethod = serverHttpRequest.getMethod();
        String language = ExchangeSupport.getRequestLanguage(exchange);
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheBody(exchange);

        if (isSkipAuth) {
            return httpOverwriteParamService.rebuildRequestBody(cacheRequestEntity.getRequestBody(), chain, exchange, serverHttpRequest.getURI());
        }

        RequestAddParamsDto requestAddParamsDto = ExchangeSupport.getRequestAddParamsDto(exchange);
        if (requestAddParamsDto == null) {
            String errorMsg = String.format("缺少需要在网关添加的参数");
            return Mono.defer(() -> {
                ResponseVo rpcResponse = ResponseVo.defaultFailureResponseVo();
                rpcResponse.setMessage("request add params attr is empty !");
                return WriteResponseSupport.shortCircuit(exchange, rpcResponse, HttpStatus.BAD_REQUEST, errorMsg);
            });
        }

        // 表单提交 处理url,不处理body
        if (mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
            URI newUri = httpOverwriteParamService.modifyParamsWithFormUrlencoded(requestAddParamsDto, serverHttpRequest, language);
            ServerHttpRequest newRequest = exchange.getRequest().mutate().uri(newUri).build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }

        URI newUri = httpOverwriteParamService.modifyQueryParams(requestAddParamsDto, serverHttpRequest, language);

        if (httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT)) {
//            String bodyCache = cacheRequestEntity.getRequestBody();
//            JSONObject body = JSON.parseObject(StringUtils.isBlank(bodyCache) ? bodyCache : Strings.EMPTY);
//            body.put(USER_ID_FIELD, requestAddParamsDto.getUserId());
            return httpOverwriteParamService.rebuildRequestBody("", chain, exchange, newUri);
        }

        ServerHttpRequest newRequest = exchange.getRequest().mutate().uri(newUri).build();
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

}
