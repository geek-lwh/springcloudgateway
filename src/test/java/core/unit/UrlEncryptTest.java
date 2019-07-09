package core.unit;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.support.URISupport;
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
    private String secretKey = "4470c4bd3d88be85f031cce6bd907329";

    private String timestamp = "1";

    @Test
    @DisplayName("GET请求加密测试类目")
    public void encryptGetRequest() {
        logger.info("<<<< {} 开始 [GET请求加密测试类目]", DateUtil.currentDateByDefaultFormat());
        String url = "http://openapi2.ahaschool.com.cn/v3/orderbff/products/all?course_propertys=1%2C2%2C4&app_types=1&city_name=%E4%B8%8A%E6%B5%B7%E5%B8%82&act_type=%E6%95%B0%E5%AD%A6&sort_type=4&limit=10&cursor=&kid_age=&_=1562232195105";

        String signature = "7beaf857946a271468fd0446793dc837";
        String timestamp = "1562232195152";
        String content = "794c7c85679ff37a79a359624eea66bc";
        String version = "Froyo";

        URI uri = UriComponentsBuilder.fromUri(URI.create(url))
                .build(true)
                .toUri();


        String rawQuery = uri.getRawQuery();
        MultiValueMap<String, String> queryParams = URISupport.initQueryParams(rawQuery);
        String sortQueryParams = URISupport.queryParamsSort(queryParams);

        String encryptStr = encryptUrl(uri.getRawPath(), sortQueryParams, timestamp, secretKey, signature);

        Asserts.check(encryptStr.equals(signature), "签名与后端加密不一致");

    }
    /**
     * 2e2613fb4da281081214c805124c0730
     */
    @Test
    @DisplayName("body加密测试类目")
    public void createContentSignature() throws IOException {
        String content = "6062124e5c23902372f704006e4009bd";
        logger.info("<<<< {} 开始 [body加密测试类目]", DateUtil.currentDateByDefaultFormat());

        String body = "{\"avatar\":\"https://wx.qlogo.cn/mmopen/vi_32/Q0j4TwGTfTKsym35icUIsx0IVE4RdOVCgOXo9hmw3rA5jgsy6icr1z37bC71F5t5GjvWicbJOeRe2VOm4yFaWfia4g/132\",\"name\":\"芊芊心结\uD83D\uDCAF\",\"gender\":2,\"city_name\":\"\",\"country\":\"China\",\"province\":\"Shanghai\",\"app_types\":\"5\",\"stack\":\"pages/me/index/index->\"}";
//        body = body.trim().replaceAll("\n","").trim();
        byte[] base64Body = Base64.encodeBase64(body.getBytes(StandardCharsets.UTF_8));
        String encodeBody = new String(base64Body, StandardCharsets.UTF_8);
        Long timestamp = 1562661403786L;

        String encryptBody = encryptBody(encodeBody, String.valueOf(timestamp), secretKey);
        System.out.println(encryptBody);
        System.out.println(encryptBody.equals(content));
    }

}
