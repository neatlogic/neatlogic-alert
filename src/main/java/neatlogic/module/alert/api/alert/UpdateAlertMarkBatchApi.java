package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ADMIN;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.exception.alert.AlertHasNotAuthException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/** 批量修改选中或匹配告警的标签，复用原有处理权限和修改记录。 */
@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateAlertMarkBatchApi extends PrivateApiComponentBase {
    @Resource
    private IAlertService alertService;

    @Override
    public String getToken() {
        return "alert/mark/update/batch";
    }

    @Override
    public String getName() {
        return "批量修改告警标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    /** 校验目标互斥和嵌套业务约束后提交标签修改。 */
    @Input({
            @Param(name = "idList", desc = "选中告警ID列表", type = ApiParamType.JSONARRAY),
            @Param(name = "searchParam", desc = "匹配搜索条件", type = ApiParamType.JSONOBJECT),
            @Param(name = "mode", desc = "修改模式", type = ApiParamType.STRING, rule = "append,replace", isRequired = true),
            @Param(name = "markNameList", desc = "标签名称列表，替换模式允许空列表", type = ApiParamType.JSONARRAY, isRequired = true)
    })
    @Description(desc = "批量修改告警标签")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        JSONArray ids = jsonObj.getJSONArray("idList");
        JSONObject searchParam = jsonObj.getJSONObject("searchParam");
        if ((ids == null) == (searchParam == null)) {
            throw new ParamIrregularException("idList/searchParam");
        }
        String mode = jsonObj.getString("mode");
        JSONArray marks = jsonObj.getJSONArray("markNameList");
        if ("append".equals(mode) && CollectionUtils.isEmpty(marks)) {
            throw new ParamIrregularException("markNameList");
        }
        LinkedHashSet<String> markNames = new LinkedHashSet<>();
        if (marks != null) {
            for (Object mark : marks) {
                if (!(mark instanceof String) || StringUtils.isBlank((String) mark)) {
                    throw new ParamIrregularException("markNameList");
                }
                markNames.add((String) mark);
            }
        }
        List<String> markNameList = new ArrayList<>(markNames);
        if (ids != null) {
            if (ids.isEmpty()) {
                throw new ParamIrregularException("idList");
            }
            List<Long> idList = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                Long id = ids.getLong(i);
                if (id == null) {
                    throw new ParamIrregularException("idList");
                }
                idList.add(id);
            }
            return alertService.batchUpdateAlertMark(idList, markNameList, mode);
        }
        if (!AuthActionChecker.check(ALERT_ADMIN.class)) {
            throw new AlertHasNotAuthException();
        }
        if (!hasValidRule(searchParam.getJSONObject("rule"))) {
            throw new ParamIrregularException("searchParam.rule");
        }
        alertService.submitMatchedAlertMarkUpdate(JSON.toJavaObject(searchParam, AlertVo.class), markNameList, mode);
        return null;
    }

    /** 确保高级条件均可参与查询，避免忽略无效条件后扩大修改范围。 */
    private boolean hasValidRule(JSONObject rule) {
        if (rule == null) {
            return false;
        }
        JSONArray groups = rule.getJSONArray("conditionGroupList");
        if (CollectionUtils.isEmpty(groups)) {
            return false;
        }
        for (int i = 0; i < groups.size(); i++) {
            JSONObject group = groups.getJSONObject(i);
            if (group == null) {
                return false;
            }
            JSONArray conditions = group.getJSONArray("conditionList");
            if (CollectionUtils.isEmpty(conditions)) {
                return false;
            }
            for (int j = 0; j < conditions.size(); j++) {
                JSONObject condition = conditions.getJSONObject(j);
                if (condition == null || StringUtils.isBlank(condition.getString("id"))
                        || StringUtils.isBlank(condition.getString("expression"))) {
                    return false;
                }
                String expression = condition.getString("expression");
                if ("is-null".equals(expression) || "is-not-null".equals(expression)) {
                    continue;
                }
                if (!Arrays.asList("equal", "notequal", "gt", "lt", "gte", "lte", "like", "notlike", "range").contains(expression)) {
                    return false;
                }
                JSONArray values = condition.getJSONArray("valueList");
                if (CollectionUtils.isEmpty(values) || ("range".equals(expression) && values.size() != 2)) {
                    return false;
                }
                for (Object value : values) {
                    if (value == null || StringUtils.isBlank(value.toString())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
