package neatlogic.module.alert.api.alertaudit;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertAuditVo;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

/** 验证标签审计历史格式兼容仅作用于查询响应。 */
public class SearchAlertAuditApiTest {
    /** 历史嵌套、新事件扁平、清空标签和其他属性均保持可展示且不回写数据库。 */
    @Test
    public void normalizesOnlyLabelAuditValuesInResponse() throws Exception {
        AlertAuditVo historical = row("const_markList", "[[\"高频\"]]", "[[\"高频\",\"测试\"]]");
        AlertAuditVo event = row("const_markList", "[]", "[\"哈哈哈\"]");
        AlertAuditVo cleared = row("const_markList", "[\"测试\"]", "[]");
        AlertAuditVo nullValue = row("const_markList", null, "[null]");
        AlertAuditVo other = row("const_status", "[[\"old\"]]", "[[\"new\"]]");
        JSONArray historicalSource = historical.getNewValueList();
        JSONArray otherSource = other.getNewValueList();
        List<AlertAuditVo> rows = Arrays.asList(historical, event, cleared, nullValue, other);
        SearchAlertAuditApi api = new SearchAlertAuditApi();
        Field mapper = SearchAlertAuditApi.class.getDeclaredField("alertAuditMapper");
        mapper.setAccessible(true);
        mapper.set(api, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertAuditMapper.class}, (proxy, method, args) -> {
            if ("selectAlertAuditCount".equals(method.getName())) {
                return rows.size();
            }
            if ("selectAlertAudit".equals(method.getName())) {
                return rows;
            }
            throw new AssertionError("查询审计不能执行其他读写：" + method.getName());
        }));
        JSONObject request = new JSONObject();
        request.put("alertId", 1L);
        JSONObject response = (JSONObject) api.myDoService(request);
        Assert.assertEquals(rows, response.get("tbodyList"));
        Assert.assertEquals(JSONArray.parseArray("[\"高频\"]"), historical.getOldValueList());
        Assert.assertEquals(JSONArray.parseArray("[\"高频\",\"测试\"]"), historical.getNewValueList());
        Assert.assertEquals(JSONArray.parseArray("[[\"高频\",\"测试\"]]"), historicalSource);
        Assert.assertTrue(event.getOldValueList().isEmpty());
        Assert.assertEquals(JSONArray.parseArray("[\"哈哈哈\"]"), event.getNewValueList());
        Assert.assertTrue(cleared.getNewValueList().isEmpty());
        Assert.assertTrue(nullValue.getOldValueList().isEmpty());
        Assert.assertTrue(nullValue.getNewValueList().isEmpty());
        Assert.assertSame(otherSource, other.getNewValueList());
        Assert.assertEquals(JSONArray.parseArray("[[\"new\"]]"), other.getNewValueList());
    }

    /** 构造数据库返回的审计数据，null保留为没有值的历史格式。 */
    private AlertAuditVo row(String attrName, String oldValues, String newValues) {
        AlertAuditVo vo = new AlertAuditVo();
        vo.setAttrName(attrName);
        if (oldValues != null) {
            vo.setOldValueList(JSONArray.parseArray(oldValues));
        }
        if (newValues != null) {
            vo.setNewValueList(JSONArray.parseArray(newValues));
        }
        return vo;
    }
}
