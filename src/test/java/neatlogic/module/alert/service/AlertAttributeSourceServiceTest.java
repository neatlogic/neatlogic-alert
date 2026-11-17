package neatlogic.module.alert.service;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.auth.core.AuthBase;
import neatlogic.framework.auth.core.AuthFactory;
import java.util.Map;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.AlertUserVo;
import neatlogic.framework.alert.dto.AlertTeamVo;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.alert.utils.AlertEventHandlerContextBuilder;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.junit.After;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import static org.junit.Assert.*;

/** 预览使用真实事件构造器，验证联系人取值与事件建单一致。 */
public class AlertAttributeSourceServiceTest {
    /** 清理当前线程的鉴权上下文。 */
    @After public void cleanup() { UserContext.get().release(); }

    /** 邮箱、电话、组成员和负责人都来自同一事件上下文，同时覆盖空列表和鉴权。 */
    @Test public void previewMatchesEventContacts() throws Exception {
        UserContext.init((UserContext) null).setUserUuid("preview-user");
        UserContext.get().setIsSuperAdmin(true);
        UserContext.get().setAuthenticationInfoVo(new AuthenticationInfoVo());
        AlertVo base = new AlertVo(); base.setId(1L);
        AlertVo full = new AlertVo(); full.setId(1L);
        AlertUserVo user = new AlertUserVo(); user.setUserId("user"); user.setUserEmail("user@example.org"); user.setUserPhone("123");
        AlertUserVo leader = new AlertUserVo(); leader.setUserId("leader"); leader.setUserPhone("456");
        AlertTeamVo team = new AlertTeamVo(); team.setTeamUuid("ops"); team.setTeamEmail("ops@example.org"); team.setTeamPhone("789");
        boolean[] empty = {false};
        var builder = new AlertEventHandlerContextBuilder();
        var service = new AlertAttributeSourceService();
        Object events = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertEventMapper.class}, (p,m,a) -> {
            return switch (m.getName()) {
                case "getAlertById" -> full;
                case "getAlertUserByAlertId", "getAlertUserByTeamId" -> empty[0] ? List.of() : List.of(user);
                case "getAlertTeamByAlertId" -> empty[0] ? List.of() : List.of(team);
                case "getAlertLeaderByTeamId" -> List.of(leader);
                default -> throw new AssertionError(m.getName());
            };
        });
        int[] queries = {0};
        Object alerts = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{AlertMapper.class}, (p,m,a) -> {
            queries[0]++;
            return Long.valueOf(1).equals(a[0]) ? base : null;
        });
        inject(builder, "alertEventMapper", events);
        inject(service, "alerts", alerts); inject(service, "contextBuilder", builder);
        JSONObject preview = service.values(service.sample(1L));
        assertEquals(service.values(builder.build(base)), preview);
        assertEquals(List.of("user@example.org"), preview.get("const_userEmailList"));
        assertEquals(List.of("user@example.org"), preview.get("const_teamUserEmailList"));
        assertEquals(List.of("ops@example.org"), preview.get("const_teamEmailList"));
        assertEquals(List.of("789", "456"), preview.get("const_teamAndLeaderPhoneList"));
        empty[0] = true;
        assertEquals(List.of(), service.values(service.sample(1L)).get("const_userEmailList"));
        assertThrows(AlertNotFoundException.class, () -> service.sample(2L));
        int previous = queries[0];
        UserContext.init((UserContext) null);
        Field authField = AuthFactory.class.getDeclaredField("authMap");
        authField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String, AuthBase> authMap = (Map<String, AuthBase>) authField.get(null);
        AuthBase previousAuth = authMap.put("ALERT_BASE", new ALERT_BASE());
        try {
            assertThrows(PermissionDeniedException.class, () -> service.sample(1L));
        } finally {
            if (previousAuth == null) authMap.remove("ALERT_BASE"); else authMap.put("ALERT_BASE", previousAuth);
        }
        assertEquals(previous, queries[0]);
    }

    /** 为实际服务注入测试边界。 */
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
}
