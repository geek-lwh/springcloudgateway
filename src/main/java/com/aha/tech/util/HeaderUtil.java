package com.aha.tech.util;

import com.aha.tech.commons.symbol.Separator;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2020/11/26
 */
public class HeaderUtil {

    /**
     * 格式化输出httpheaders
     * @param httpHeaders
     * @return
     */
    public static String formatHttpHeaders(HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return Strings.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        httpHeaders.forEach((String k, List<String> v) -> sb.append(k).append(Separator.COLON_MARK).append(StringUtils.collectionToDelimitedString(v, Separator.COMMA_MARK)).append(System.lineSeparator()));

        return sb.toString();
    }
}
