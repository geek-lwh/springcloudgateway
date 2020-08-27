package com.aha.tech.core.support;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.constant.SystemConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @Author: luweihong
 * @Date: 2020/7/20
 */
public class VersionSupport {

    private static final Logger logger = LoggerFactory.getLogger(VersionSupport.class);

    private static final String STR_PREFIX = "ahaschool";

    private static final String STR_PREFIX2 = "ahakid";


    /**
     * 版本号比较
     * version1 == version2 return 0
     * version1 < version2  return -1
     * version1 > version2 return 1
     *
     * @param version1
     * @param version2
     * @return
     */
    public static int compareVersion(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        String[] version1Array = StringUtils.tokenizeToStringArray(version1, ".");
        String[] version2Array = StringUtils.tokenizeToStringArray(version2, ".");
        int index = 0;
        // 获取最小长度值
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;
        // 循环判断每位的大小
        while (index < minLen
                && (diff = Integer.parseInt(version1Array[index])
                - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            // 如果位数不一致，比较多余位数
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }

    /**
     * 判断是否是app请求
     * @param userAgent
     * @return
     */
    public static Boolean isAppRequest(String userAgent) {
        String tmp = userAgent.toLowerCase();
        if(tmp.startsWith(STR_PREFIX) || tmp.startsWith(STR_PREFIX2)){
            return Boolean.TRUE;
        }

        return Boolean.FALSE;
    }

    /**
     * 解析出os和version
     * @param agent
     * @return
     */
    public static String[] parseOsAndVersion(String agent) {
        String os = SystemConstant.WEB_CLIENT;
        String version = SystemConstant.DEFAULT_VERSION;
        if (isAppRequest(agent)) {
            String[] tmp = StringUtils.tokenizeToStringArray(agent, Separator.SLASH_MARK);
            if (tmp.length > 3) {
                os = tmp[1];
                version = tmp[2];
            } else {
                logger.warn("is an app request,but agent is wrong : {}", agent);
            }
        }

        return new String[]{os, version};
    }


    public static void main(String[] args) {
        System.out.println("=====>");
        System.out.println(VersionSupport.compareVersion("5.1.4", "6.1.6"));
        System.out.println(VersionSupport.compareVersion("7.1", "6.1.6"));
        System.out.println(VersionSupport.compareVersion("2.2", "1.1.1.1"));
    }
}
