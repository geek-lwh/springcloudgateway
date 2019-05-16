package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.OverwriteParamService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.WriteResponseSupport;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Map;

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
        String cacheBody = cacheRequestEntity.getRequestBody();

        if (isSkipAuth) {
            return httpOverwriteParamService.rebuildRequestBody(cacheBody, chain, exchange, serverHttpRequest.getURI());
        }

        RequestAddParamsDto requestAddParamsDto = ExchangeSupport.getRequestAddParamsDto(exchange);
        if (requestAddParamsDto == null) {
            String errorMsg = String.format("缺少需要在网关添加的参数");
            return Mono.defer(() -> {
                ResponseVo rpcResponse = ResponseVo.defaultFailureResponseVo();
                rpcResponse.setMessage("request add params attr is empty !");
                return WriteResponseSupport.shortCircuit(exchange, rpcResponse, errorMsg);
            });
        }

        // 表单提交 处理url,不处理body
        URI newUri = httpOverwriteParamService.modifyQueryParams(requestAddParamsDto, serverHttpRequest, language);

//        // 表单提交
//        if (mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
//            ServerHttpRequest newRequest = exchange.getRequest().mutate().uri(newUri).build();
//            return chain.filter(exchange.mutate().request(newRequest).build());
//        }

        Boolean hasBody = httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT);
        if (hasBody && mediaType.isCompatibleWith(MediaType.APPLICATION_JSON_UTF8)) {
            Map<String, Object> map = Maps.newHashMap();
            if (StringUtils.isBlank(cacheBody)) {
                map.put(USER_ID_FIELD, requestAddParamsDto.getUserId());
            } else {
                map = JSON.parseObject(cacheRequestEntity.getRequestBody(), Map.class);
                map.put(USER_ID_FIELD, requestAddParamsDto.getUserId());
            }
            cacheBody = JSON.toJSONString(map);
        }

        return httpOverwriteParamService.rebuildRequestBody(cacheBody, chain, exchange, newUri);
    }

}
