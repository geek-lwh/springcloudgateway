package com.aha.tech.core.support;

import com.aha.tech.commons.symbol.Separator;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.MultiValueMap;
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
     * get请求的query params 排序
     * @param queryParams
     * @return
     */
    public static String queryParamsSort(MultiValueMap<String, String> queryParams) {
        List<String> sortQueryParamsList = Lists.newArrayList();
        queryParams.forEach((String paramName, List<String> paramValueList) -> {
            if (paramName.equals(Separator.UNDER_LINE_MARK)) {
                return;
            }

            for (String paramValue : paramValueList) {
                String str = String.format("%s=%s", paramName, StringUtils.isEmpty(paramValue) ? Strings.EMPTY : paramValue);
                sortQueryParamsList.add(str);
            }
        });

        if (CollectionUtils.isEmpty(sortQueryParamsList)) {
            return Strings.EMPTY;
        }

        Collections.sort(sortQueryParamsList);

        String sortQueryParamsStr = StringUtils.collectionToDelimitedString(sortQueryParamsList, Separator.AND_MARK);
        logger.debug("queryParams 排序后 : {}", sortQueryParamsStr);

        return sortQueryParamsStr;
    }

    /**
     * url 加密算法
     * @param rawPath
     * @param sortQueryParamsStr
     * @param timestamp
     * @param secretKey
     * @return
     */
    public static String encryptUrl(String rawPath, String sortQueryParamsStr, String timestamp, String secretKey) {
        String lastMd5 = Strings.EMPTY;
        try {
            String str1 = rawPath + sortQueryParamsStr + timestamp;
            String firstMd5 = DigestUtils.md5DigestAsHex(str1.getBytes());
            logger.debug("原串 : {} , 第一次md5 : {}", str1, firstMd5);
            String str2 = firstMd5 + secretKey;
            lastMd5 = DigestUtils.md5DigestAsHex(str2.getBytes());
            logger.debug("第二次 md5 : {},secret_key : {}", lastMd5, secretKey);
        } catch (Exception e) {
            logger.error("url防篡改加密出现异常,raw_path={},sort_raw_query={},timestamp={}", rawPath, sortQueryParamsStr, timestamp, e);
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
