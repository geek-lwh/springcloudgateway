package com.aha.tech.core.support;

import com.aha.tech.commons.symbol.Separator;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.util.*;
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
     *
     * @param paraMap
     * @param urlEncode
     * @param keyToLower
     * @return
     */
    public static String formatUrlMap(Map<String, String> paraMap, boolean urlEncode, boolean keyToLower) {
        String buff;
        Map<String, String> tmpMap = paraMap;
        try {
            List<Map.Entry<String, String>> infoIds = new ArrayList<>(tmpMap.entrySet());
            // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
            Collections.sort(infoIds, Comparator.comparing(o -> (o.getKey())));
            // 构造URL 键值对的格式
            StringBuilder buf = new StringBuilder();
            for (Map.Entry<String, String> item : infoIds) {
                if (!StringUtils.isEmpty(item.getKey())) {
                    String key = item.getKey();
                    String val = item.getValue();
                    if (urlEncode) {
                        val = URLEncoder.encode(val, "utf-8");
                    }
                    if (keyToLower) {
                        buf.append(key.toLowerCase() + "=" + val);
                    } else {
                        buf.append(key + "=" + val);
                    }
                    buf.append("&");
                }

            }
            buff = buf.toString();
            if (buff.isEmpty() == false) {
                buff = buff.substring(0, buff.length() - 1);
            }
        } catch (Exception e) {
            return null;
        }
        return buff;
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
        Map<String, String> queryMaps = Maps.newHashMap();

        String lastMd5 = Strings.EMPTY;
        String sortQueryParamsStr = Strings.EMPTY;
        try {
            if (!StringUtils.isEmpty(rawQuery)) {
                String[] paramArr = rawQuery.split(Separator.AND_MARK);
                for (String param : paramArr) {
                    String[] kv = param.split(Separator.EQUAL_SIGN_MARK);
                    String k = kv[0];
                    // url中前端ajax会拼接_=1555395225473 这种query
                    if (k.equals("_")) {
                        continue;
                    }

                    queryMaps.put(kv[0], kv[1]);
                }
                sortQueryParamsStr = formatUrlMap(queryMaps, false, false);
            }
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
