package com.aha.tech.core.filters.global;

import com.aha.tech.core.model.dto.BaggageItemDto;
import com.aha.tech.core.model.entity.SnapshotRequestEntity;
import com.aha.tech.core.service.OverwriteParamService;
import com.aha.tech.core.support.AttributeSupport;
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
import static com.aha.tech.passportserver.facade.constants.AuthorizationServerConstants.ANONYMOUS_KID_ID;
import static com.aha.tech.passportserver.facade.constants.AuthorizationServerConstants.ANONYMOUS_USER_ID;

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

    private static final String KID_ID_FIELD = "kid_id";

    @Override
    public int getOrder() {
        return MODIFY_PARAMS_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return replaceRequest(exchange, chain);
    }

    /**
     * 重构params
     * @param exchange
     * @param chain
     * @return
     */
    private Mono<Void> replaceRequest(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();
        MediaType mediaType = httpHeaders.getContentType();

        HttpMethod httpMethod = serverHttpRequest.getMethod();
        SnapshotRequestEntity snapshotRequestEntity = AttributeSupport.getSnapshotRequest(exchange);
        String cacheBody = snapshotRequestEntity.getRequestBody();
        BaggageItemDto baggageItemDto = load(exchange);
        URI newUri = httpOverwriteParamService.modifyQueryParams(baggageItemDto, serverHttpRequest);
        Boolean needAddBodyParams = httpMethod.equals(HttpMethod.POST) || httpMethod.equals(HttpMethod.PUT);
        if (!needAddBodyParams) {
            ServerHttpRequest request = exchange.getRequest().mutate().uri(newUri).build();
            return chain.filter(exchange.mutate().request(request).build());
        }

        String newBodyStr = cacheBody;

        if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON_UTF8)) {
            Long userId = baggageItemDto.getUserId();
            Long kidId = baggageItemDto.getKidId();
            Map<String, Object> map = Maps.newHashMap();
            if (StringUtils.isNotBlank(cacheBody)) {
                map = JSON.parseObject(cacheBody, Map.class);
                map.put(USER_ID_FIELD, userId);
                map.put(KID_ID_FIELD, kidId);
            }

            newBodyStr = JSON.toJSONString(map);
        }

        return httpOverwriteParamService.rebuildRequestBody(newBodyStr, chain, exchange, newUri);
    }

    /**
     * 获取exchange中缓存的数据
     * @param exchange
     * @return
     */
    private BaggageItemDto load(ServerWebExchange exchange) {
        BaggageItemDto baggageItemDto = AttributeSupport.getRequestAddParamsDto(exchange);

        if (baggageItemDto == null || baggageItemDto.getUserId() == null) {
            logger.warn("缺失user_id,默认为游客访问");
            baggageItemDto.setUserId(ANONYMOUS_USER_ID);
        }

        if (baggageItemDto == null || baggageItemDto.getKidId() == null) {
            logger.warn("user_id : {} 缺少kid_id,默认设置为0", baggageItemDto.getUserId());
            baggageItemDto.setKidId(ANONYMOUS_KID_ID);
        }
        return baggageItemDto;
    }

}
