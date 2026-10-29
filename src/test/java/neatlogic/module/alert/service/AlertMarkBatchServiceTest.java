package neatlogic.module.alert.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertAuditVo;
import neatlogic.framework.alert.dto.AlertUserVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.exception.alert.AlertHasNotAuthException;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertMarkMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

/** 使用替身隔离数据库及ES，验证标签更新和权限等真实业务路径。 */
public class AlertMarkBatchServiceTest {
    private final AlertServiceImpl service = new AlertServiceImpl();
    private final Map<Long, AlertVo> alerts = new HashMap<>();
    private final List<AlertAuditVo> audits = new ArrayList<>();
    private final List<String> writes = new ArrayList<>();
    private Map<String, IElasticsearchDocument> indexMap;
    private IElasticsearchDocument previousIndex;
    private boolean failFirst;
    private Field authUserMapperField;
    private Object previousAuthUserMapper;

    /** 注入数据库和索引替身，并初始化审计用户。 */
    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        UserContext context = UserContext.init((UserContext) null);
        context.setUserUuid("mark-test-user");
        context.setTimezone("UTC");
        context.setIsSuperAdmin(true);
        context.setAuthenticationInfoVo(new AuthenticationInfoVo());
        InputFromContext.init(InputFrom.UNKNOWN);
        // 当前鉴权实现始终读取授权Mapper，保存并恢复静态替身以隔离外部数据库。
        authUserMapperField = AuthActionChecker.class.getDeclaredField("userMapper");
        authUserMapperField.setAccessible(true);
        previousAuthUserMapper = authUserMapperField.get(null);
        authUserMapperField.set(null, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{UserMapper.class}, (proxy, method, args) -> {
            if ("searchUserAllAuthByUserAuth".equals(method.getName())) {
                return Collections.emptyList();
            }
            throw new AssertionError("不应读取额外用户数据：" + method.getName());
        }));
        inject("alertMapper", Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertMapper.class}, (proxy, method, args) -> {
            if ("getAlertById".equals(method.getName())) {
                return alerts.get(args[0]);
            }
            throw new AssertionError("标签修改不能调用其他告警写入方法：" + method.getName());
        }));
        inject("alertMarkMapper", Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertMarkMapper.class}, (proxy, method, args) -> {
            if (failFirst && "deleteAlertAlertMarkByAlertId".equals(method.getName()) && Objects.equals(1L, args[0])) {
                throw new IllegalStateException("模拟单条失败");
            }
            writes.add(method.getName());
            return null;
        }));
        inject("alertAuditMapper", Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertAuditMapper.class}, (proxy, method, args) -> {
            audits.add((AlertAuditVo) args[0]);
            return null;
        }));
        inject("applicationContext", Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ApplicationContext.class}, (proxy, method, args) -> service));
        Field mapField = ElasticsearchDocumentFactory.class.getDeclaredField("componentMap");
        mapField.setAccessible(true);
        indexMap = (Map<String, IElasticsearchDocument>) mapField.get(null);
        previousIndex = indexMap.put("ALERT", (IElasticsearchDocument) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{IElasticsearchDocument.class}, (proxy, method, args) -> {
            if ("isIndexExists".equals(method.getName())) {
                return true;
            }
            if ("getIndexName".equals(method.getName())) {
                return "test-alert";
            }
            if ("updateDocument".equals(method.getName()) || "createDocument".equals(method.getName())) {
                writes.add(method.getName());
            }
            return null;
        }));
    }

    /** 恢复全局索引注册和线程上下文，避免污染其他用例。 */
    @After
    public void cleanup() throws Exception {
        if (authUserMapperField != null) {
            authUserMapperField.set(null, previousAuthUserMapper);
        }
        if (indexMap != null) {
            if (previousIndex == null) {
                indexMap.remove("ALERT");
            } else {
                indexMap.put("ALERT", previousIndex);
            }
        }
        if (UserContext.get() != null) {
            UserContext.get().release();
        }
        if (InputFromContext.get() != null) {
            InputFromContext.get().release();
        }
    }

    /** 追加保留旧标签并去重；审计记录原标签及当前操作用户。 */
    @Test
    public void appendKeepsOldLabelsAndWritesAuditOnlyForLabels() {
        AlertVo old = alert(1L, "old");
        old.setStatus("critical");
        old.setIsClose(1);
        JSONObject counts = service.batchUpdateAlertMark(Arrays.asList(1L, 1L), Arrays.asList("new", "old", "new"), "append");
        Assert.assertEquals(1, counts.getIntValue("updatedCount"));
        Assert.assertEquals(1, audits.size());
        AlertAuditVo audit = audits.get(0);
        Assert.assertEquals("const_markList", audit.getAttrName());
        Assert.assertEquals("mark-test-user", audit.getInputUser());
        Assert.assertEquals(Collections.singletonList("old"), audit.getOldValueList());
        Assert.assertEquals(Arrays.asList("old", "new"), audit.getNewValueList());
        Assert.assertEquals("critical", old.getStatus());
        Assert.assertEquals(1, old.getIsClose());
        Assert.assertEquals(Collections.singletonList("old"), old.getMarkNameList());
        Assert.assertTrue(writes.contains("updateDocument"));
        Assert.assertTrue(writes.contains("createDocument"));
    }

    /** 原详情处理入口仍沿用相同标签审计，状态和关闭状态不变时不写其他字段。 */
    @Test
    public void originalHandleAlertKeepsLabelAuditBehavior() {
        AlertVo old = alert(1L, "old");
        old.setStatus("critical");
        old.setIsClose(0);
        AlertVo request = new AlertVo();
        request.setId(1L);
        request.setStatus("critical");
        request.setIsClose(0);
        request.setMarkNameList(Collections.singletonList("new"));
        service.handleAlert(request);
        Assert.assertEquals(1, audits.size());
        AlertAuditVo audit = audits.get(0);
        Assert.assertEquals("const_markList", audit.getAttrName());
        Assert.assertEquals("mark-test-user", audit.getInputUser());
        Assert.assertEquals(Collections.singletonList("old"), audit.getOldValueList());
        Assert.assertEquals(Collections.singletonList("new"), audit.getNewValueList());
        Assert.assertTrue(writes.contains("createDocument"));
        Assert.assertEquals("critical", old.getStatus());
        Assert.assertEquals(0, old.getIsClose());
    }

    /** 没有历史标签时，新审计旧值写空数组而不是null或嵌套空值。 */
    @Test
    public void missingOldLabelsAreRecordedAsEmptyArray() {
        AlertVo old = alert(1L);
        old.setMarkNameList(null);
        JSONObject counts = service.batchUpdateAlertMark(Collections.singletonList(1L), Collections.singletonList("new"), "replace");
        Assert.assertEquals(1, counts.getIntValue("updatedCount"));
        Assert.assertEquals(Collections.emptyList(), audits.get(0).getOldValueList());
        Assert.assertEquals(Collections.singletonList("new"), audits.get(0).getNewValueList());
    }

    /** 空替换可以清空，集合相同或已失效目标不会写关系和审计。 */
    @Test
    public void emptyReplaceAndUnchangedAndExpiredAreCounted() {
        alert(1L, "old");
        alert(2L);
        AlertVo deleted = alert(3L, "deleted");
        deleted.setIsDelete(1);
        JSONObject counts = service.batchUpdateAlertMark(Arrays.asList(1L, 2L, 3L, 4L), Collections.emptyList(), "replace");
        Assert.assertEquals(1, counts.getIntValue("updatedCount"));
        Assert.assertEquals(1, counts.getIntValue("unchangedCount"));
        Assert.assertEquals(2, counts.getIntValue("skippedCount"));
        Assert.assertEquals(1, audits.size());
        Assert.assertEquals(Collections.emptyList(), audits.get(0).getNewValueList());
    }

    /** 替换移除旧标签；仅顺序不同的相同集合不产生修改记录。 */
    @Test
    public void replaceDropsOldLabelsAndEqualSetsDoNotWrite() {
        alert(1L, "old");
        alert(2L, "one", "two");
        JSONObject replaced = service.batchUpdateAlertMark(Collections.singletonList(1L), Arrays.asList("new", "new"), "replace");
        Assert.assertEquals(1, replaced.getIntValue("updatedCount"));
        Assert.assertEquals(Collections.singletonList("new"), audits.get(0).getNewValueList());
        writes.clear();
        audits.clear();
        JSONObject unchanged = service.batchUpdateAlertMark(Collections.singletonList(2L), Arrays.asList("two", "one"), "replace");
        Assert.assertEquals(1, unchanged.getIntValue("unchangedCount"));
        Assert.assertTrue(writes.isEmpty());
        Assert.assertTrue(audits.isEmpty());
    }

    /** 第二条无权限时整个批次零写入，第一条有权限也不能先修改。 */
    @Test
    public void permissionPrecheckPreventsAllWrites() {
        UserContext.get().setIsSuperAdmin(false);
        AlertVo first = alert(1L, "old");
        AlertUserVo user = new AlertUserVo();
        user.setUserId("mark-test-user");
        first.setUserList(Collections.singletonList(user));
        alert(2L, "old");
        try {
            service.batchUpdateAlertMark(Arrays.asList(1L, 2L), Collections.singletonList("new"), "replace");
            Assert.fail("应拒绝无权限告警");
        } catch (AlertHasNotAuthException expected) {
            Assert.assertTrue(writes.isEmpty());
            Assert.assertTrue(audits.isEmpty());
        }
    }

    /** 一条失败后继续下一条并分别统计。 */
    @Test
    public void failureDoesNotStopFollowingAlerts() {
        failFirst = true;
        alert(1L, "old");
        alert(2L, "old");
        JSONObject counts = service.batchUpdateAlertMark(Arrays.asList(1L, 2L), Collections.singletonList("new"), "replace");
        Assert.assertEquals(1, counts.getIntValue("failedCount"));
        Assert.assertEquals(1, counts.getIntValue("updatedCount"));
        Assert.assertEquals(1, audits.size());
        Assert.assertEquals(Long.valueOf(2), audits.get(0).getAlertId());
    }

    /** 构造数据库读取的告警，不额外生成测试之外的业务状态。 */
    private AlertVo alert(Long id, String... names) {
        AlertVo alert = new AlertVo();
        alert.setId(id);
        alert.setMarkNameList(Arrays.asList(names));
        alerts.put(id, alert);
        return alert;
    }

    /** 仅在测试中注入替身，不增加生产装配接口。 */
    private void inject(String name, Object value) throws Exception {
        Field field = AlertServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
