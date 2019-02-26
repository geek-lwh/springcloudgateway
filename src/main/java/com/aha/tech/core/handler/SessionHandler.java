package com.aha.tech.core.handler;

import com.aha.tech.passportserver.facade.model.vo.UserVo;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 *
 * 存储调用授权系统后的用户视图对象
 */
public class SessionHandler {

    private static final ThreadLocal<UserVo> threadLocal = ThreadLocal.withInitial(() -> null);

    public static UserVo get(){
        UserVo userVo = threadLocal.get();
        return userVo;
    }

    public static void remove(){
        threadLocal.remove();
    }
    public static void set(UserVo userVo){
        threadLocal.set(userVo);
    }

}
