//package com.aha.tech.config;
//
//import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
//import org.apache.commons.lang3.builder.ToStringStyle;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * @Author: luweihong
// * @Date: 2019/3/13
// *
// * 网关白名单
// */
//@Configuration
////@ConfigurationProperties(prefix = "skip.user.white.list")
//public class UserIdWhiteListConfiguration {
//
//    @Value("skip.user.white.list")
//    private String userList;
//
//    @Bean("userWhiteList")
//    public String userWhiteList() {
//        return userList;
//    }
//
//    public String getUserList() {
//        return userList;
//    }
//
//    public void setUserList(String userList) {
//        this.userList = userList;
//    }
//
//    @Override
//    public String toString() {
//        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
//    }
//}
