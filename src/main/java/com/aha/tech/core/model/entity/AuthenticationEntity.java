package com.aha.tech.core.model.entity;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.passportserver.facade.model.vo.UserVo;

/**
 * @Author: luweihong
 * @Date: 2019/3/13
 *
 * 鉴权实体类
 */
public class AuthenticationEntity {

    private RpcResponse<UserVo> rpcResponse;

    private String userName;

    private String password;

    private Boolean verifyResult;

    public AuthenticationEntity() {
        super();
    }

    public RpcResponse<UserVo> getRpcResponse() {
        return rpcResponse;
    }

    public void setRpcResponse(RpcResponse<UserVo> rpcResponse) {
        this.rpcResponse = rpcResponse;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getVerifyResult() {
        return verifyResult;
    }

    public void setVerifyResult(Boolean verifyResult) {
        this.verifyResult = verifyResult;
    }
}
