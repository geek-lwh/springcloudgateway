package core.unit;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.support.UriSupport;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.aha.tech.core.support.UriSupport.buildRewritePath;

/**
 * @Author: luweihong
 * @Date: 2019/4/2
 */
@DisplayName("模拟网关重写uri地址测试类目")
public class RewritePathTest {

    private RouteEntity routeEntity;

    private String input;

    private String output;

    @Before
    public void initRequest() {
        input = "/v3/yanxuan/banner/get";
        output = "hjm_order_war/yanxuan/banner/get";

        routeEntity = new RouteEntity();
        routeEntity.setId("yanxuan");
        routeEntity.setUri(output);
        routeEntity.setContextPath("hjm_order_war");
    }

    @Test
    @DisplayName("去除请求url中以下划线分割的第一个区位测试类目")
    public void excludeInvalidPath() {
        System.out.println("请求进入的url : " + input);
        String validPath = UriSupport.excludeStrings(input, Separator.SLASH_MARK, 1);
        String verifyStr = "yanxuan/banner/get";
        System.out.println("入参 : " + validPath);
        System.out.println("期望值 : " + verifyStr);
        Assertions.assertThat(validPath).as("校验失败").isEqualTo(verifyStr);
        System.out.println("去除无效路径后,url=" + validPath);
    }

    @Test
    @DisplayName("重写uri测试类目")
    public void rewritePathTest() {
        String validPath = "yanxuan/banner/get";
        String id = StringUtils.substringBefore(validPath, Separator.SLASH_MARK);
        System.out.println("入参 : " + id);
        System.out.println("期望值 : " + routeEntity.getId());
        Assertions.assertThat(id).as("重写后与期望值不匹配").isEqualTo(routeEntity.getId());

        String contextPath = routeEntity.getContextPath();
        String rewritePath = buildRewritePath(contextPath, validPath);
        System.out.println("入参 : " + rewritePath);
        System.out.println("期望值 : " + output);
        Assertions.assertThat(rewritePath).as("重写后与期望值不匹配").isEqualTo(output);
    }
}
