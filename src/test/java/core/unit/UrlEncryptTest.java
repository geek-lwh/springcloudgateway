package core.unit;

import com.aha.tech.commons.utils.DateUtil;
import com.google.common.collect.Maps;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

import static com.aha.tech.core.support.UriSupport.*;

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
    @DisplayName("url加密测试类目")
    public void urlEncryptTest() {
        logger.info("<<<< {} 开始 [url加密测试类目]", DateUtil.currentDateByDefaultFormat());
        URI url = URI.create("http://openapi2.ahaschool.com.cn/v3/account/quota?z=20&c=2&a=1&_=1555395225473");
        String rawQuery = url.getRawQuery();
        System.out.println(rawQuery.equals(null));
        String rawPath = url.getRawPath();

        String encryptStr = encryptUrl(rawPath, rawQuery, timestamp, secretKey);
        System.out.print("加密后值 : " + encryptStr);
    }

    @Test
    @DisplayName("body加密测试类目")
    public void bodyEncryptTest() {
        logger.info("<<<< {} 开始 [body加密测试类目]", DateUtil.currentDateByDefaultFormat());
        String body = "{\"user_id\":2134}";

        String encryptStr = encryptBody(body, timestamp, secretKey);
        System.out.print("加密后的值 : " + encryptStr);
    }


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
