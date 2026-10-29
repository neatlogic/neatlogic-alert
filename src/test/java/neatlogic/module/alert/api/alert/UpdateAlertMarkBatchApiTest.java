package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.exception.alert.AlertHasNotAuthException;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.module.alert.service.IAlertService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** 验证批量标签入口的范围保护及空替换约定。 */
public class UpdateAlertMarkBatchApiTest {
    /** 清理测试用户，避免污染其他用例。 */
    @After
    public void cleanup() {
        if (UserContext.get() != null) {
            UserContext.get().release();
        }
    }

    /** 目标来源不能同时指定，避免接口静默选择其中一种。 */
    @Test(expected = ParamIrregularException.class)
    public void rejectsAmbiguousTarget() {
        JSONObject request = request();
        request.put("idList", JSONArray.parseArray("[1]"));
        request.put("searchParam", new JSONObject());
        new UpdateAlertMarkBatchApi().myDoService(request);
    }

    /** 追加没有标签应拒绝，不提交任何修改。 */
    @Test(expected = ParamIrregularException.class)
    public void rejectsEmptyAppend() {
        JSONObject request = request();
        request.put("mode", "append");
        request.put("idList", JSONArray.parseArray("[1]"));
        new UpdateAlertMarkBatchApi().myDoService(request);
    }

    /** 空替换明确传入空标签列表，以便清空现有标签。 */
    @Test
    public void emptyReplaceIsForwarded() throws Exception {
        UpdateAlertMarkBatchApi api = new UpdateAlertMarkBatchApi();
        AtomicBoolean called = new AtomicBoolean();
        Field field = UpdateAlertMarkBatchApi.class.getDeclaredField("alertService");
        field.setAccessible(true);
        field.set(api, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IAlertService.class}, (proxy, method, args) -> {
            Assert.assertEquals("batchUpdateAlertMark", method.getName());
            Assert.assertEquals(Arrays.asList(1L), args[0]);
            Assert.assertTrue(((List<?>) args[1]).isEmpty());
            Assert.assertEquals("replace", args[2]);
            called.set(true);
            return new JSONObject();
        }));
        JSONObject request = request();
        request.put("idList", JSONArray.parseArray("[1]"));
        api.myDoService(request);
        Assert.assertTrue(called.get());
    }

    /** 即使高级条件有效，普通处理人也不能发起匹配修改。 */
    @Test(expected = AlertHasNotAuthException.class)
    public void matchedUpdateRequiresAdministrator() {
        JSONObject request = request();
        request.put("searchParam", new JSONObject());
        new UpdateAlertMarkBatchApi().myDoService(request);
    }

    /** 未知表达式可能被搜索忽略，必须拒绝以免扩大匹配范围。 */
    @Test(expected = ParamIrregularException.class)
    public void rejectsIgnoredRuleExpression() {
        initAdmin();
        JSONObject request = request();
        request.put("searchParam", JSONObject.parseObject("{\"rule\":{\"conditionGroupList\":[{\"conditionList\":[{\"id\":\"const_markList\",\"expression\":\"unknown\",\"valueList\":[\"old\"]}]}]}}"));
        new UpdateAlertMarkBatchApi().myDoService(request);
    }

    /** 没有高级条件时不能执行全量匹配修改。 */
    @Test(expected = ParamIrregularException.class)
    public void rejectsMissingAdvancedRule() {
        initAdmin();
        JSONObject request = request();
        request.put("searchParam", new JSONObject());
        new UpdateAlertMarkBatchApi().myDoService(request);
    }

    /** 创建替换为空的标准请求。 */
    private JSONObject request() {
        JSONObject request = new JSONObject();
        request.put("mode", "replace");
        request.put("markNameList", new JSONArray());
        return request;
    }

    /** 使用无外部依赖的管理员上下文测试条件保护。 */
    private void initAdmin() {
        UserContext context = UserContext.init((UserContext) null);
        context.setUserUuid("mark-test-user");
        context.setTimezone("UTC");
        context.setIsSuperAdmin(true);
    }
}
