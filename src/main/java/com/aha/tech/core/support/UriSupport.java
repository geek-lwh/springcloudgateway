package com.aha.tech.core.support;

import com.aha.tech.commons.symbol.Separator;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: luweihong
 * @Date: 2019/4/2
 */
public class UriSupport {

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
     * @param path
     * @return
     */
    public static String buildRewritePath(String contextPath, String path) {
        String rewritePath = new StringBuilder()
                .append(contextPath)
                .append(Separator.SLASH_MARK)
                .append(path).toString();

        return rewritePath;
    }
}
