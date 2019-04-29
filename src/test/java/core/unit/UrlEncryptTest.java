package core.unit;

import com.aha.tech.commons.utils.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.Base64;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.aha.tech.core.support.URISupport.*;

/**
 * @Author: luweihong
 * @Date: 2019/4/16
 */
@DisplayName("url加密")
public class UrlEncryptTest {

    private static final Logger logger = LoggerFactory.getLogger(UrlEncryptTest.class);

    private String secretKey = "d1f1bd03e3b0e08d6ebbecaa60e14445";

    private String timestamp = "1555395428000";

    @Test
    @DisplayName("生成signature测试类目")
    public void createXSignature() {
        logger.info("<<<< {} 开始 [生成signature测试类目]", DateUtil.currentDateByDefaultFormat());
        URI url = URI.create("http://api-test5.d.ahaschool.com/v3/userbff/visitor/devices/create");
        String rawQuery = url.getRawQuery();
        String rawPath = url.getRawPath();

        String encryptStr = encryptUrl(rawPath, rawQuery, timestamp, secretKey);
        System.out.print("加密后值 : " + encryptStr);
    }

    /**
     * 2e2613fb4da281081214c805124c0730
     */
    @Test
    @DisplayName("body加密测试类目")
    public void createContentSignature() throws IOException {
        logger.info("<<<< {} 开始 [body加密测试类目]", DateUtil.currentDateByDefaultFormat());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_id", 2134);
        String body = jsonObject.toJSONString();
        byte[] base64Body = Base64.encodeBase64(body.getBytes());
        String encodeBody = new String(base64Body, StandardCharsets.UTF_8);


        String encryptBody = encryptBody(encodeBody, timestamp, secretKey);
        System.out.println(encryptBody.equals(encodeBody));
    }

//     "message": "url防篡改校验失败,参数:[uri=http://api-test5.d.ahaschool.com/v3/userbff/visitor/devices/create,version=Froyo,timestamp=1556440724503,content=6ecd95c8c403732ed7ed90f501ce599c,signature=5cbb0122804ed76722573533bd4c4d62]"



    @Test
    @DisplayName("GET请求加密测试类目")
    public void encryptGetRequest() {
        logger.info("<<<< {} 开始 [GET请求加密测试类目]", DateUtil.currentDateByDefaultFormat());
        URI url = URI.create("http://openapi2.ahaschool.com.cn/v3/account/quota?wallet_type=2&_=1555395225473");
        String subUrl = url.getRawPath();
        Assertions.assertThat(subUrl).as("/v3/account/quota").isEqualTo(true);

        String queryParams = url.getRawQuery();
        Assertions.assertThat(queryParams).as("wallet_type=2&_=1555395225473").isEqualTo(true);

        String[] params = queryParams.split("&");
        Map<String, String> queryMaps = Maps.newHashMap();
        for (String s : params) {
            String[] str = s.split("=");
            queryMaps.put(str[0], str[1]);
        }

        String s = formatUrlMap(queryMaps, false, false);
        System.out.print("s : " + s);
    }


}
