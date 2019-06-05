package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.service.OverwriteParamService;
import com.aha.tech.core.support.ExchangeSupport;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
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

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        MediaType mediaType = httpHeaders.getContentType();

        HttpMethod httpMethod = serverHttpRequest.getMethod();
        String language = ExchangeSupport.getRequestLanguage(exchange);
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheBody(exchange);
        String cacheBody = cacheRequestEntity.getRequestBody();

        RequestAddParamsDto requestAddParamsDto = ExchangeSupport.getRequestAddParamsDto(exchange);

        URI newUri = httpOverwriteParamService.modifyQueryParams(requestAddParamsDto, serverHttpRequest, language);
        Boolean needAddBodyParams = httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT);
        if (!needAddBodyParams) {
            ServerHttpRequest request = exchange.getRequest().mutate().uri(newUri).build();
            return chain.filter(exchange.mutate().request(request).build());
        }

        // MediaType.APPLICATION_FORM_URLENCODED 不转json
        String newBodyStr = cacheBody;

        if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON_UTF8)) {
            Long userId = requestAddParamsDto.getUserId();
            Map<String, Object> map = Maps.newHashMap();
            if (StringUtils.isNotBlank(cacheBody)) {
                map = JSON.parseObject(cacheBody, Map.class);
            }
            if (userId != null) {
                map.put(USER_ID_FIELD, userId);
            }

            newBodyStr = JSON.toJSONString(map);
        }

        return httpOverwriteParamService.rebuildRequestBody(newBodyStr, chain, exchange, newUri);
    }

}
