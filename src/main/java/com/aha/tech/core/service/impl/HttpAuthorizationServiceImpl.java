package com.aha.tech.core.service.impl;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.exception.AuthorizationFailedException;
import com.aha.tech.core.exception.VisitorAccessTokenException;
import com.aha.tech.core.exception.VisitorNotInWhiteListException;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.service.AuthorizationService;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private Map<String, List<String>> whiteListMap;

    /**
     * 校验访客信息的合法性
     * @param accessToken
     * @return
     */
    @Override
    public AuthenticationEntity verifyVisitorAccessToken(String id, String path, String accessToken) {
        AuthenticationEntity authenticationEntity = new AuthenticationEntity();
        authenticationEntity.setVerifyResult(Boolean.TRUE);

        Boolean checkTokenValid = accessToken.equals(DEFAULT_X_TOKEN_VALUE);
        if (!checkTokenValid) {
            logger.warn("匿名用户令牌不合法, access token : {}", accessToken);
            throw new VisitorAccessTokenException();
        }

        // 如果是访客 校验是否接口再白名单中,是则允许访问
        Boolean existWhiteList;
        List<String> list = whiteListMap.containsKey(id) ? whiteListMap.get(id) : Collections.emptyList();
        if (!CollectionUtils.isEmpty(list) && list.get(0).equals(Separator.ASTERISK_MARK)) {
            logger.debug("该列表配置的白名单为*,允许所有请求通过");
            existWhiteList = Boolean.TRUE;
        } else {
            existWhiteList = list.contains(path);
        }

        if (!existWhiteList) {
            logger.error("游客访问资源不在白名单列表中 : {}", path);
            throw new VisitorNotInWhiteListException();
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
            throw new AuthorizationFailedException(code, rpcResponse.getMessage());
        }

        authenticationEntity.setVerifyResult(code == SUCCESS);
        authenticationEntity.setUserVo(rpcResponse.getData());

        return authenticationEntity;
    }

}
