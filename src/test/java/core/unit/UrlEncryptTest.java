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

    //    private String secretKey = "4470c4bd3d88be85f031cce6bd907329";
    private String secretKey = "d1f1bd03e3b0e08d6ebbecaa60e14445";

    private String timestamp = "1";

    @Test
    @DisplayName("GET请求加密测试类目")
    public void encryptGetRequest() {
        logger.info("<<<< {} 开始 [GET请求加密测试类目]", DateUtil.currentDateByDefaultFormat());
        String url = "http://10.10.189.191:9700/v3/orderbff/orders/paycert/get?pay_type=6&order_id=4223274&openid=&return_url=https%3A%2F%2Ftestm.ahaschool.com%2Forder%2Fdetail%3Forder_id%3D4223274%26product_id%3D502656%26alipaysuccess%3D1%26utm_source%3D%26utm_medium%3D%26utm_campaign%3D%26utm_term%3D%26utm_content%3D%26pk%3D%26pd%3D%26ps%3D%26pp%3D&_=1559550174584";

        String signature = "7c3f335437f066927753499e5ace75a1";
        String timestamp = "1559550186041";
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
