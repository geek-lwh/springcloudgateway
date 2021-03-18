package com.aha.tech.core.service.impl;

import com.aha.tech.commons.constants.ResponseConstants;
import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.model.dto.BaggageItemDto;
import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import com.aha.tech.core.service.AuthorizationService;
import com.aha.tech.passportserver.facade.code.AuthorizationCode;
import com.aha.tech.passportserver.facade.constants.AuthorizationServerConstants;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import javax.annotation.Resource;

import static com.aha.tech.core.constant.AttributeConstant.GATEWAY_REQUEST_ADD_PARAMS_ATTR;
import static com.aha.tech.core.constant.HeaderFieldConstant.DEFAULT_X_TOKEN_VALUE;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 *
 */
@Service("httpAuthorizationService")
public class HttpAuthorizationServiceImpl implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(HttpAuthorizationServiceImpl.class);

    @Resource
    private PassportResource passportResource;

    /**
     * 校验访客信息的合法性
     * @param swe
     * @param accessToken
     * @return
     */
    @Override
    public AuthenticationResultEntity verifyVisitorAccessToken(ServerWebExchange swe, String accessToken) {
        AuthenticationResultEntity authenticationResultEntity = new AuthenticationResultEntity();
        Boolean checkTokenValid = accessToken.equals(DEFAULT_X_TOKEN_VALUE);
        if (!checkTokenValid) {
            logger.warn("匿名用户令牌不合法, access token : {}", accessToken);
            authenticationResultEntity.setMessage("匿名用户令牌不合法");
        }

        authenticationResultEntity.setCode(checkTokenValid ? ResponseConstants.SUCCESS : ResponseConstants.FAILURE);
        BaggageItemDto requestAddParamsDto = new BaggageItemDto();
        requestAddParamsDto.setUserId(UserVo.anonymousUser().getUserId());
        requestAddParamsDto.setKidId(AuthorizationServerConstants.ANONYMOUS_KID_ID);
        swe.getAttributes().put(GATEWAY_REQUEST_ADD_PARAMS_ATTR, requestAddParamsDto);

        return authenticationResultEntity;
    }

    /**
     * 校验用户信息合法性
     * @param exchange
     * @param accessToken
     * @return
     */
    @Override
    public AuthenticationResultEntity verifyUser(ServerWebExchange exchange, String accessToken) {
        AuthenticationResultEntity authenticationResultEntity = new AuthenticationResultEntity();
        RpcResponse<UserVo> rpcResponse = passportResource.verify(accessToken);
        Integer code = rpcResponse.getCode();
        authenticationResultEntity.setMessage(rpcResponse.getMessage());
        authenticationResultEntity.setCode(code);

        // 如果是5300或者0 都传递userId和kidId
        if (code.equals(ResponseConstants.SUCCESS) || code.equals(AuthorizationCode.WRONG_KID_ACCOUNT_CODE)) {
            UserVo userVo = rpcResponse.getData();
            BaggageItemDto requestAddParamsDto = new BaggageItemDto();
            requestAddParamsDto.setUserId(userVo.getUserId());
            requestAddParamsDto.setKidId(userVo.getKidId());
            exchange.getAttributes().put(GATEWAY_REQUEST_ADD_PARAMS_ATTR, requestAddParamsDto);
        }

        return authenticationResultEntity;
    }

}
