package com.aha.tech.core.constant;

/**
 * @Author: luweihong
 * @Date: 2019/6/13
 */
public class SystemConstant {

    public final static String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    public final static String PROD = "prod";

    public final static String UAT = "uat";

    public final static String TEST = "test";

    // 在这个版本之前与之后useragent是否需要额外的逻辑处理
    public final static String FIX_AHA_KID_USER_AGENT_VERSION = "6.1.6";

    // 兼容5300
    public final static String COMPATIBILITY_5300_VERSION = "7.1.0";

    public final static String WEB_CLIENT = "web";

    public final static String ANDROID_CLIENT = "android";

    public final static String IOS_CLIENT = "ios";

    public final static String DEFAULT_VERSION = "10";

    public final static String DEFAULT_ERROR_MESSAGE = "我好像开了个小差!";

    public final static String APPLICATION_PORT = "9700";

    public final static String APPLICATION_NAME = "GATEWAY_SERVER";
}
