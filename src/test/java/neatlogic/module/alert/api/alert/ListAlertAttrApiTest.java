package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.restful.groupsearch.core.GroupSearchOptionVo;
import neatlogic.framework.restful.groupsearch.core.GroupSearchVo;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.framework.groupsearch.handler.CommonGroupHandler;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

/** 验证审计属性独立于列表展示、搜索和视图过滤。 */
public class ListAlertAttrApiTest {
    /** 视图人员条件开放系统当前用户，处理组和事件 UUID 条件不扩展。 */
    @Test
    public void viewUserConditionOffersOnlySupportedDynamicUser() {
        AlertAttrDefineVo user = AlertAttr.getSearchConstAttrList().stream()
                .filter(a -> "const_userList".equals(a.getName())).findFirst().get();
        JSONObject config = user.getConfig();
        Assert.assertEquals(java.util.Arrays.asList("user", "common"), config.getJSONArray("groupList"));
        Assert.assertEquals(Collections.singletonList("common#loginuser"), config.getJSONArray("includeList"));
        Assert.assertEquals(Collections.singletonList("common#alluser"), config.getJSONArray("excludeList"));
        // 用系统选择器实际查询和回显，验证配置不会放出其他动态用户类型。
        GroupSearchVo search = config.toJavaObject(GroupSearchVo.class);
        CommonGroupHandler handler = new CommonGroupHandler();
        List<GroupSearchOptionVo> options = handler.search(search);
        Assert.assertEquals(1, options.size());
        Assert.assertEquals("common#loginuser", options.get(0).getValue());
        search.setValueList(Collections.singletonList("common#loginuser"));
        List<GroupSearchOptionVo> reloaded = handler.reload(search);
        Assert.assertEquals(1, reloaded.size());
        Assert.assertEquals(options.get(0).getValue(), reloaded.get(0).getValue());
        Assert.assertEquals(options.get(0).getText(), reloaded.get(0).getText());
        for (String name : new String[]{"const_teamList", "const_teamUuidList", "const_userUuidList"}) {
            AlertAttrDefineVo unchanged = AlertAttr.getAllConstAttrList().stream()
                    .filter(a -> name.equals(a.getName())).findFirst().get();
            Assert.assertFalse(unchanged.getConfig().containsKey("includeList"));
            Assert.assertFalse(unchanged.getConfig().getJSONArray("groupList").contains("common"));
        }
    }

    /** 审计包含标签及隐藏扩展属性，同时不改变原有列表和搜索属性过滤。 */
    @Test
    @SuppressWarnings("unchecked")
    public void auditIncludesAllAttributesWithoutChangingColumnFlags() throws Exception {
        ListAlertAttrApi api = new ListAlertAttrApi();
        AlertAttrTypeVo hidden = new AlertAttrTypeVo();
        hidden.setId(1L);
        hidden.setName("hidden");
        hidden.setLabel("隐藏属性");
        hidden.setIsShow(0);
        hidden.setIsIndex(0);
        Field mapper = ListAlertAttrApi.class.getDeclaredField("alertAttrTypeMapper");
        mapper.setAccessible(true);
        mapper.set(api, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertAttrTypeMapper.class}, (proxy, method, args) -> Collections.singletonList(hidden)));
        AlertAttrDefineVo originalMark = AlertAttr.getAllConstAttrList().stream().filter(a -> "const_markList".equals(a.getName())).findFirst().get();
        Assert.assertFalse(originalMark.isColumn());
        JSONObject auditRequest = new JSONObject();
        auditRequest.put("isAudit", 1);
        auditRequest.put("isColumn", 1);
        auditRequest.put("isSearch", 1);
        auditRequest.put("viewName", "view-filter-must-not-hide-audit-fields");
        List<AlertAttrDefineVo> audit = (List<AlertAttrDefineVo>) api.myDoService(auditRequest);
        Assert.assertTrue(audit.stream().anyMatch(a -> "const_markList".equals(a.getName())));
        Assert.assertTrue(audit.stream().anyMatch(a -> "attr_hidden".equals(a.getName())));
        Assert.assertEquals(AlertAttr.getAllConstAttrList().size() + 1, audit.size());
        JSONObject columnRequest = new JSONObject();
        columnRequest.put("isColumn", 1);
        List<AlertAttrDefineVo> columns = (List<AlertAttrDefineVo>) api.myDoService(columnRequest);
        Assert.assertFalse(columns.stream().anyMatch(a -> "const_markList".equals(a.getName())));
        Assert.assertFalse(columns.stream().anyMatch(a -> "attr_hidden".equals(a.getName())));
        Assert.assertEquals(AlertAttr.getColumnConstAttrList().size(), columns.size());
        JSONObject searchRequest = new JSONObject();
        searchRequest.put("isSearch", 1);
        List<AlertAttrDefineVo> search = (List<AlertAttrDefineVo>) api.myDoService(searchRequest);
        Assert.assertFalse(search.stream().anyMatch(a -> "attr_hidden".equals(a.getName())));
        Assert.assertEquals(AlertAttr.getSearchConstAttrList().size(), search.size());
        Assert.assertFalse(originalMark.isColumn());
    }
}
