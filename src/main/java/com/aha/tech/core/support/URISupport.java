package com.aha.tech.core.support;

import com.aha.tech.commons.symbol.Separator;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: luweihong
 * @Date: 2019/4/2
 */
public class URISupport {

    private static final Logger logger = LoggerFactory.getLogger(URISupport.class);

    private final static String SPECIAL_SYMBOL = "_=";

    /**
     * 根据字符串,切割符,跳过无效区位数
     * @param str
     * @param Separator
     * @param skipPart
     * @return
     */
    public static String excludeStrings(String str, String Separator, int skipPart) {
        String[] arr = StringUtils.tokenizeToStringArray(str, Separator);
        Stream<String> validPathStream = Arrays.stream(arr).skip(skipPart);
        String afterExclude = validPathStream.collect(Collectors.joining(Separator));

        return afterExclude;
    }

    /**
     * 构建重写后的路由地址
     * @param contextPath
     * @param realServerHost
     * @return
     */
    public static String buildRewritePath(String contextPath, String realServerHost) {
        StringBuilder rewritePath = new StringBuilder();

        if (StringUtils.isEmpty(contextPath)) {
            contextPath = Separator.SLASH_MARK;
        }

        if (!StringUtils.startsWithIgnoreCase(contextPath, Separator.SLASH_MARK)) {
            contextPath = Separator.SLASH_MARK + contextPath;
        }

        rewritePath.append(contextPath).append(realServerHost).toString();

        return rewritePath.toString();
    }

    /**
     * url 加密算法
     * @param rawPath
     * @param rawQuery
     * @param timestamp
     * @param secretKey
     * @return
     */
    public static String encryptUrl(String rawPath, String rawQuery, String timestamp, String secretKey) {
        String lastMd5 = Strings.EMPTY;
        String sortQueryParamsStr = Strings.EMPTY;
        try {
            if (!StringUtils.isEmpty(rawQuery)) {
                String[] paramArr = rawQuery.split(Separator.AND_MARK);
                List<String> list = Lists.newArrayList(paramArr);
                Collections.sort(list);
                for (String v : list) {
                    if (v.startsWith(SPECIAL_SYMBOL)) {
                        continue;
                    }
                    sortQueryParamsStr += v;
                }
            }

            logger.debug("after sort params : {}", sortQueryParamsStr);
            String str1 = rawPath + sortQueryParamsStr + timestamp;
            String firstMd5 = DigestUtils.md5DigestAsHex(str1.getBytes());
            String str2 = firstMd5 + secretKey;
            lastMd5 = DigestUtils.md5DigestAsHex(str2.getBytes());
        } catch (Exception e) {
            logger.error("url防篡改加密出现异常,raw_path={},rawQuery={},timestamp={}", rawPath, rawQuery, timestamp, e);
        }


        return lastMd5;
    }

    /**
     * body加密
     * @param encodeBody
     * @param timestamp
     * @param secretKey
     * @return
     */
    public static String encryptBody(String encodeBody, String timestamp, String secretKey) {
        String lastMd5 = Strings.EMPTY;
        try {
            if (StringUtils.isEmpty(encodeBody)) {
                logger.error("body防篡改加密时出现body为空");
                return Strings.EMPTY;
            }

            String str1 = encodeBody + timestamp;
            String firstMd5 = DigestUtils.md5DigestAsHex(str1.getBytes());

            String str2 = firstMd5 + secretKey;
            lastMd5 = DigestUtils.md5DigestAsHex(str2.getBytes());
        } catch (Exception e) {
            logger.error("body防篡改加密出现异常 body={},timestamp={}", encodeBody, timestamp, e);
        }

        return lastMd5;
    }

}
