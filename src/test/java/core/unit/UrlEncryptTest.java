package core.unit;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.support.URISupport;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.util.Asserts;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

    @Test
    @DisplayName("GET请求加密测试类目")
    public void encryptGetRequest() {
        logger.info("<<<< {} 开始 [GET请求加密测试类目]", DateUtil.currentDateByDefaultFormat());
//       =http://api-test.d.ahaschool.com/v3/appbff/course/screen/all?category_id=3&search_list=11%2C10,version=Froyo,timestamp=1557371770667,content=1d798d4314ec45365b3e0ae33b09a172,signature=699eb686f0b5ba1c711a2a322758a36b
        URI url = URI.create("http://api-test.d.ahaschool.com/v3/appbff/course/screen/all?category_id=3&search_list=11%2C10");
        String signature = "699eb686f0b5ba1c711a2a322758a36b";
        String timestamp = "1557371770667";
        String content = "b266f3154977f5dd6da84591b28fe0db";
        String version = "Froyo";

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.put("category_id", Lists.newArrayList("3"));
        queryParams.put("search_list", Lists.newArrayList("11%2C10"));

        String sortQueryParams = URISupport.queryParamsSort(queryParams);

        String encryptStr = encryptUrl(url.getRawPath(), sortQueryParams, timestamp, secretKey);

        Asserts.check(encryptStr.equals(signature), "签名与后端加密不一致");

    }


}
