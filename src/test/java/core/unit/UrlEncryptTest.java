package core.unit;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.support.URISupport;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.util.Asserts;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.aha.tech.core.support.URISupport.encryptBody;
import static com.aha.tech.core.support.URISupport.encryptUrl;

/**
 * @Author: luweihong
 * @Date: 2019/4/16
 */
@DisplayName("url加密")
public class UrlEncryptTest {

    private static final Logger logger = LoggerFactory.getLogger(UrlEncryptTest.class);

    private String secretKey = "4470c4bd3d88be85f031cce6bd907329";
//    private String secretKey = "d1f1bd03e3b0e08d6ebbecaa60e14445";

    private String timestamp = "1";

    @Test
    @DisplayName("GET请求加密测试类目")
    public void encryptGetRequest() {
        logger.info("<<<< {} 开始 [GET请求加密测试类目]", DateUtil.currentDateByDefaultFormat());
        String url = "https://mobileapi.ahaschool.com.cn/v3/userbff/visitor/devices/create";

        String signature = "3f837cbfe1075bdb0591e9f49ef291b3";
        String timestamp = "1559024917656";
        String content = "b266f3154977f5dd6da84591b28fe0db";
        String version = "Froyo";

        URI uri = UriComponentsBuilder.fromUri(URI.create(url))
                .build(true)
                .toUri();


        String rawQuery = uri.getRawQuery();
        MultiValueMap<String, String> queryParams = URISupport.initQueryParams(rawQuery);
        String sortQueryParams = URISupport.queryParamsSort(queryParams);

        String encryptStr = encryptUrl(uri.getRawPath(), sortQueryParams, timestamp, secretKey);

        Asserts.check(encryptStr.equals(signature), "签名与后端加密不一致");

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

}
