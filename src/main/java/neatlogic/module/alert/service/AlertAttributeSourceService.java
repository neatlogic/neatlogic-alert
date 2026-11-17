package neatlogic.module.alert.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.utils.AlertEventHandlerContextBuilder;
import neatlogic.framework.alert.crossover.IAlertAttributeSourceService;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.exception.type.PermissionDeniedException;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

/** 属性发现与取值共用告警模型；扩展属性定义在当前租户下读取。 */
@Service
public class AlertAttributeSourceService implements IAlertAttributeSourceService {
    @Resource private AlertAttrTypeMapper attributes;
    @Resource private AlertMapper alerts;
    @Resource private AlertEventHandlerContextBuilder contextBuilder;

    /** 固定属性取自底座，过滤仅用于聚合模板的虚拟字段。 */
    @Override public List<JSONObject> definitions(Long alertTypeId) {
        List<JSONObject> result = new ArrayList<>();
        for (AlertAttrDefineVo def : AlertAttr.getAllConstAttrList()) {
            if (def.getName().startsWith("const_")) {
                result.add(new JSONObject().fluentPut("name", def.getName()).fluentPut("label", def.getLabel())
                        .fluentPut("kind", "const").fluentPut("type", def.getType() == null ? "text" : def.getType())
                        .fluentPut("config", def.getConfig()));
            }
        }
        Set<Long> recommended = alertTypeId == null ? Collections.emptySet() : new HashSet<>(attributes.listAssociatedIds(alertTypeId));
        List<AlertAttrTypeVo> extensions = new ArrayList<>(attributes.listAttrType());
        extensions.sort(Comparator.comparing(attr -> !recommended.contains(attr.getId())));
        for (AlertAttrTypeVo attr : extensions) {
            result.add(new JSONObject().fluentPut("id", attr.getId().toString()).fluentPut("name", "attr_" + attr.getName())
                    .fluentPut("label", attr.getLabel()).fluentPut("kind", "attr").fluentPut("type", attr.getType())
                    .fluentPut("recommended", recommended.contains(attr.getId())).fluentPut("config", attr.getConfig()));
        }
        return result;
    }

    /** 部分通知模板字段没有 VO getter，按相同人员和组关系取值。 */
    @Override public JSONObject values(AlertVo alert) {
        JSONObject bean = (JSONObject) JSON.toJSON(alert);
        JSONObject values = new JSONObject();
        for (AlertAttrDefineVo def : AlertAttr.getAllConstAttrList()) {
            if (def.getName().startsWith("const_")) values.put(def.getName(), bean.get(def.getName().substring(6)));
        }
        List<?> users = bean.getJSONArray("userList");
        List<?> teams = bean.getJSONArray("teamList");
        for (String key : List.of("Account", "Email", "Phone")) {
            values.put("const_user" + key + "List", project(users, "user" + key));
        }
        List<Object> members = flatten(teams, "userList");
        for (String key : List.of("Account", "Email", "Phone", "Name")) {
            values.put("const_teamUser" + key + "List", project(members, "user" + key));
        }
        values.put("const_teamEmailList", project(teams, "teamEmail"));
        values.put("const_teamPhoneList", project(teams, "teamPhone"));
        List<Object> phones = project(teams, "teamPhone");
        phones.addAll(project(flatten(teams, "leaderList"), "userPhone"));
        values.put("const_teamAndLeaderPhoneList", phones);
        if (alert.getAttrObj() != null) alert.getAttrObj().forEach((key, value) -> values.put("attr_" + key, value));
        return values;
    }

    /** 按告警中的原始顺序投影人员或组属性。 */
    private static List<Object> project(List<?> rows, String key) {
        List<Object> result = new ArrayList<>();
        if (rows != null) for (Object row : rows) {
            if (row instanceof Map && ((Map<?, ?>) row).get(key) != null) result.add(((Map<?, ?>) row).get(key));
        }
        return result;
    }

    /** 组内成员或负责人列表保留所属组的顺序。 */
    private static List<Object> flatten(List<?> rows, String key) {
        List<Object> result = new ArrayList<>();
        for (Object value : project(rows, key)) if (value instanceof List) result.addAll((List<?>) value);
        return result;
    }

    /** 先验证查看权限，再复用真实事件上下文补齐联系人和组关系。 */
    @Override public AlertVo sample(Long id) throws PermissionDeniedException {
        if (!AuthActionChecker.check(ALERT_BASE.class)) throw new PermissionDeniedException(ALERT_BASE.class);
        AlertVo alert = alerts.getAlertById(id);
        if (alert == null) throw new AlertNotFoundException(id);
        return contextBuilder.build(alert);
    }
}
