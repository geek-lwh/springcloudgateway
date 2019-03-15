package com.aha.tech.core.model.dto;

import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 *
 * 网关拦截请求添加的参数信息
 */
public class Params {

    private Long userId;

    public Params(){
        super();
    }

    public Params(UserVo userVo) {
        this.userId = userVo.getUserId();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString(){
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
