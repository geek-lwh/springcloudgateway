package com.aha.tech.core.controller.resource.fallback;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.passportserver.facade.model.dto.AuthorizationRequestDto;
import com.aha.tech.passportserver.facade.model.dto.KillSessionRequestDto;
import com.aha.tech.passportserver.facade.model.vo.AccessTokenVo;
import com.aha.tech.passportserver.facade.model.vo.SessionVo;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/1/9
 */
@Component
public class PassportFallbackFactory implements FallbackFactory<PassportResource> {

    private static Logger logger = LoggerFactory.getLogger(PassportFallbackFactory.class);

    @Override
    public PassportResource create(Throwable cause) {
        return new PassportResource() {
            @Override
            public RpcResponse<AccessTokenVo> acquireAccessToken(AuthorizationRequestDto authorizationRequestDto) {
                logger.error("获取访问令牌失败,进入降级 : {}", cause);
                return RpcResponse.defaultHystrixFallbackResponse();
            }

            @Override
            public RpcResponse<UserVo> verify(String accessToken) {
                logger.error("校验访问令牌失败,进入降级 : {}", cause);
                return RpcResponse.defaultHystrixFallbackResponse();
            }

            @Override
            public RpcResponse<List<SessionVo>> sessionList(Long userId) {
                logger.error("获取用户登录信息集合失败,进入降级 : {}", cause);
                return RpcResponse.defaultHystrixFallbackResponse();
            }

            @Override
            public RpcResponse killSession(KillSessionRequestDto killSessionRequestDto) {
                logger.error("剔除用户信息失败,进入降级 : {}", cause);
                return RpcResponse.defaultHystrixFallbackResponse();
            }
        };
    }
}
