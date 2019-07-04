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
        String content = "45e8e948f8a275c7a03003e0ecd8b229";
        logger.info("<<<< {} 开始 [body加密测试类目]", DateUtil.currentDateByDefaultFormat());
        String s = "{\"openid\":\"og1vg1BZFav6pNuKJ_OxOZsAMcRA\",\"option_id\":47932,\"pattern_id\":0,\"order_count\":1,\"apply_mode\":1,\"order_type\":2,\"order_price\":9.9,\"postage_price\":0,\"coupon_id\":0,\"coupon_title\":\"暂无可用优惠券\",\"payment_from\":3,\"remark\":\"\",\"product_id\":506048,\"groupbuy_id\":0,\"referee\":\"\",\"gift_title\":\"\",\"sum_discount\":0,\"return_url\":\"https://n.ahaschool.com" +
                ".cn/groupbuy/share-guide/groupbuy_id/{groupbuy_id}/order_id/{order_id}/alipaysuccess/1?utm_source=promotion_ekysbxl&utm_medium=promotion&utm_campaign=20190703&utm_term=&utm_content=&pk=&pd=&ps=&pp=\",\"stranger_type\":0,\"form1\":\"\",\"form2\":\"\",\"address\":{\"address_id\":0,\"order_id\":0,\"country\":0,\"country_code\":\"\",\"province\":\"\",\"city_name\":\"\",\"district\":\"\",\"community_id\":0,\"community_name\":\"\",\"address\":\"\",\"poi_type\":1,\"contact_name\":\"aha3379144\",\"contact_mobile\":\"13521804870\"},\"app_type\":1,\"isInApp\":true,\"page_source\":\"product_detail\"}";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("openid", "og1vg1BZFav6pNuKJ_OxOZsAMcRA");
        jsonObject.put("option_id", 47932);
        jsonObject.put("pattern_id", 0);
        jsonObject.put("order_count", 1);
        jsonObject.put("apply_mode", 1);
        jsonObject.put("order_type", 2);
        jsonObject.put("order_price", 9.9);
        jsonObject.put("postage_price", 0);
//        jsonObject.put("coupon_id",0);url防篡改校验失败
        jsonObject.put("coupon_title", "暂无可用优惠券");
        jsonObject.put("payment_from", 3);
        jsonObject.put("remark", "");
        jsonObject.put("product_id", 506048);
        jsonObject.put("groupbuy_id", 0);
        jsonObject.put("referee", "");
        jsonObject.put("gift_title", "");
        jsonObject.put("sum_discount", 0);
        jsonObject.put("return_url", "https://n.ahaschool.com.cn/groupbuy/share-guide/groupbuy_id/{groupbuy_id}/order_id/{order_id}/alipaysuccess/1?utm_source=promotion_ekysbxl&utm_medium=promotion&utm_campaign=20190703&utm_term=&utm_content=&pk=&pd=&ps=&pp=");
        jsonObject.put("stranger_type", 0);
        jsonObject.put("form1", "");
        jsonObject.put("form2", "");


        String body = "";
        byte[] base64Body = Base64.encodeBase64(body.getBytes());
        String encodeBody = new String(base64Body, StandardCharsets.UTF_8);
        Long timestamp = 1560395388l;

        String encryptBody = encryptBody(encodeBody, String.valueOf(timestamp), secretKey);
        System.out.println(encryptBody);
        System.out.println(encryptBody.equals(content));
    }

}
