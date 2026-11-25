package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertRuleVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertRuleMapper;
import neatlogic.module.alert.service.IAlertService;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.*;

/** 固定创建告警插件已有的取值、兜底和审计行为。 */
public class AlertSaveEventHandlerTest {
    private final AlertSaveEventHandler handler = new AlertSaveEventHandler();
    private final AlertVo alert = new AlertVo();
    private final AlertEventHandlerAuditVo audit = new AlertEventHandlerAuditVo();

    /** 隔离规则查询和告警保存，避免访问租户环境。 */
    @Before
    public void setup() throws ReflectiveOperationException {
        alert.setId(7L);
        Field service = AlertSaveEventHandler.class.getDeclaredField("alertService");
        service.setAccessible(true);
        service.set(handler, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IAlertService.class},
                (proxy, method, args) -> {
                    assertEquals("saveAlert", method.getName());
                    assertSame(alert, args[0]);
                    return null;
                }));
        Field mapper = AlertSaveEventHandler.class.getDeclaredField("alertRuleMapper");
        mapper.setAccessible(true);
        mapper.set(handler, Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{AlertRuleMapper.class},
                (proxy, method, args) -> {
                    assertEquals("getAlertRuleByIdList", method.getName());
                    return List.of(rule("[0-9]+", "N"), rule("N", "slot"));
                }));
    }

    /** 先排序、再按查询结果顺序应用规则，保留值内的拼接分隔符。 */
    @Test
    public void preservesLegacyCompositionAndRuleOrder() {
        alert.setTitle("Disk 22#critical");
        alert.setAttrObj(new JSONObject().fluentPut("host", "host-a"));
        trigger("{uniqueAttrList:[{name:'const_title'},{name:'attr_missing'},{name:'attr_host'}],ruleList:[2,1]}");
        assertKey("host-a#Disk slot#critical", Md5Util.encryptMD5("host-a#Disk slot#critical"));
    }

    /** 固定属性为空时保留旧的 null 字符串，缺失的扩展属性跳过。 */
    @Test
    public void preservesNullAndMissingAttributeHandling() {
        trigger("{uniqueAttrList:[{name:'const_title'},{name:'attr_missing'}]}");
        assertKey("null", Md5Util.encryptMD5("null"));
    }

    /** 已经是 MD5 的唯一键直接沿用。 */
    @Test
    public void preservesExistingDigest() {
        String digest = Md5Util.encryptMD5("disk");
        alert.setUniqueKey(digest);
        trigger("{uniqueAttrList:[{name:'const_uniqueKey'}]}");
        assertKey(digest, digest);
    }

    /** 没有计算结果时，继续使用传入的唯一键。 */
    @Test
    public void preservesSuppliedKeyFallback() {
        alert.setUniqueKey("upstream");
        trigger("{uniqueAttrList:[{name:'attr_missing'}]}");
        assertKey("upstream", Md5Util.encryptMD5("upstream"));
    }

    /** 没有传入键或计算结果时，审计和告警使用同一个随机键。 */
    @Test
    public void preservesRandomKeyFallback() {
        trigger("{uniqueAttrList:[{name:'attr_missing'}]}");
        assertEquals(32, alert.getUniqueKey().length());
        assertKey(alert.getUniqueKey(), alert.getUniqueKey());
    }

    /** 构建仅作用于标题的正则规则。 */
    private AlertRuleVo rule(String pattern, String replacement) {
        AlertRuleVo rule = new AlertRuleVo();
        rule.setAttrName("const_title");
        rule.setConfig(new JSONObject().fluentPut("ruleList", List.of(new JSONObject()
                .fluentPut("pattern", pattern).fluentPut("replacement", replacement))));
        return rule;
    }

    /** 直接执行插件主逻辑以检查运行值和审计结果。 */
    private void trigger(String config) {
        AlertEventHandlerVo event = new AlertEventHandlerVo();
        event.setConfig(JSON.parseObject(config));
        handler.myTrigger(event, null, alert, audit, null);
    }

    /** 比对保存的键、审计键以及审计中的计算原文。 */
    private void assertKey(String original, String key) {
        assertEquals(key, alert.getUniqueKey());
        assertEquals(key, audit.getUniqueKey());
        assertEquals(key, audit.getResult().getString("uniqueKey"));
        assertEquals(original, audit.getResult().getString("uniqueKeyOriginal"));
    }
}
