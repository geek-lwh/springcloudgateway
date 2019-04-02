package com.aha.tech.core.support;

import cn.hutool.core.codec.Base64;
import com.aha.tech.commons.symbol.Separator;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.aha.tech.core.constant.HeaderFieldConstant.HEADER_X_FORWARDED_FOR;
import static com.aha.tech.core.constant.HeaderFieldConstant.X_ENV_FIELD_PP;

/**
 * @Author: luweihong
 * @Date: 2019/4/1
 *
 * 解析XEnv支持类
 */
public class ParseHeadersSupport {


    private static final String PP_ENCODE_PREFIX = X_ENV_FIELD_PP;

    private static final String PP_ENCODE_SUFFIX = "Aha^_^";

    private static final String ENCODE_PREFIX = "hjm?";

    /**
     * 校验 pp值
     * pp = pp_raw + '$' + 'pp' +md5("hjm?" + md5(string({"pp":pp_raw}) + "Aha^_^")
     * pp_raw 比较  'pp' +md5("hjm?" + md5(string({"pp":pp_raw}) + "Aha^_^")
     * $左边的值与右边的值比较是否相等
     * @param pp 解密后的明文 : 505637:100077:0:1397323:2:0$pp8b12b0faf4064fd36c333d7e3fa1e3327
     * @return
     */
    public static boolean verifyPp(String pp) {
        String[] arr = pp.split("\\$");
        String verifySource = arr[0];
        String verifyTarget = arr[1];
        // 构建一个key是pp 值是 $ 前的 json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(PP_ENCODE_PREFIX, verifySource);
        String json = jsonObject.toJSONString();

        String s = json + PP_ENCODE_SUFFIX;
        // 再将encodeJson md5
        String MD5Str = DigestUtils.md5DigestAsHex(s.getBytes(StandardCharsets.UTF_8));

        // 最后 再拼接hjm? + MD5Str + Aha^_^
        String finalStr = String.format("%s%s",ENCODE_PREFIX, MD5Str);

        // 再对finalStr 进行 md5
        String value = PP_ENCODE_PREFIX + DigestUtils.md5DigestAsHex(finalStr.getBytes(StandardCharsets.UTF_8));

        return verifyTarget.equals(value);
    }

    /**
     * 解析http头里的ip
     * @param httpHeaders
     * @return
     */
    public static String parseHeaderIp(HttpHeaders httpHeaders) {
        List<String> ipList = httpHeaders.get(HEADER_X_FORWARDED_FOR);
        if (CollectionUtils.isEmpty(ipList)) {
            return null;
        }

        String ips = ipList.get(0);
        String[] ipArr = StringUtils.tokenizeToStringArray(ips, Separator.COMMA_MARK);

        return ipArr[0];
    }

    public static void main(String[] args) {
        String s = "NTA1NjM3OjEwMDA3NzowOjEzOTczMjM6MjowJHBwOGIxMmIwZmFmNDA2NGZkMzZjMzMzZDdlM2ZhMWUzMzI=";
        String v = Base64.decodeStr(s);
        boolean a = verifyPp(v);
        System.out.println("a : " + a);
    }
}
