package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.exception.AuthorizationFailedException;
import com.aha.tech.core.exception.VisitorAccessTokenException;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.service.AuthorizationService;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;
import static com.aha.tech.core.constant.HeaderFieldConstant.DEFAULT_X_TOKEN_VALUE;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 *
 */
@Service("httpAuthorizationService")
public class HttpAuthorizationServiceImpl implements AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(HttpAuthorizationServiceImpl.class);

    @Autowired(required = false)
    private PassportResource passportResource;

    @Resource
    private List<String> whiteList;

    @Override
    public Boolean isWhiteList(String path) {
        return whiteList.contains(path);
    }

    /**
     * 校验访客信息的合法性
     * @param accessToken
     * @return
     */
    @Override
    public AuthenticationEntity verifyVisitorAccessToken(String accessToken) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        authenticationEntity.setVerifyResult(Boolean.TRUE);

        Boolean checkTokenValid = accessToken.equals(DEFAULT_X_TOKEN_VALUE);
        if (!checkTokenValid) {
            logger.warn("匿名用户令牌不合法, access token : {}", accessToken);
            throw new VisitorAccessTokenException();
        }

        authenticationEntity.setUserVo(UserVo.anonymousUser());

        return authenticationEntity;
    }

    /**
     * 校验用户信息合法性
     * @param accessToken
     * @return
     */
    @Override
    public AuthenticationEntity verifyUser(String accessToken) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        RpcResponse<UserVo> rpcResponse = passportResource.verify(accessToken);
        int code = rpcResponse.getCode();
        if (code != SUCCESS) {
            logger.error("访问令牌不合法 : {}", accessToken);
            throw new AuthorizationFailedException(code, rpcResponse.getMessage());
        }

        authenticationEntity.setVerifyResult(code == SUCCESS);
        authenticationEntity.setUserVo(rpcResponse.getData());

        return authenticationEntity;
    }

}
