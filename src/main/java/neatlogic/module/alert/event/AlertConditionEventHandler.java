/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.dto.condition.ConditionGroupVo;
import neatlogic.framework.alert.dto.condition.ConditionVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.event.IAlertEventHandler;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.util.javascript.JavascriptUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AlertConditionEventHandler extends AlertEventHandlerBase {
    private final Logger logger = LoggerFactory.getLogger(AlertConditionEventHandler.class);
    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertEventMapper alertEventMapper;

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo) {
        if (MapUtils.isNotEmpty(alertEventHandlerVo.getConfig())) {
            JSONArray eventConditionList = alertEventHandlerVo.getConfig().getJSONArray("conditionList");
            JSONArray resultConditionList = new JSONArray();
            for (int e = 0; e < eventConditionList.size(); e++) {
                JSONObject resultObj = new JSONObject();
                JSONObject eventConditionObj = eventConditionList.getJSONObject(e);
                JSONObject handlerObj = eventConditionObj.getJSONObject("handler");
                JSONObject rule = eventConditionObj.getJSONObject("rule");
                resultObj.put("rule", rule);

                boolean isValid = true;
                if (MapUtils.isNotEmpty(rule)) {
                    JSONArray conditionGroupList = rule.getJSONArray("conditionGroupList");
                    JSONArray conditionGroupRelList = rule.getJSONArray("conditionGroupRelList");
                    if (CollectionUtils.isNotEmpty(conditionGroupList)) {
                        //构造脚本
                        StringBuilder script = new StringBuilder();
                        JSONObject conditionObj = new JSONObject();
                        for (int i = 0; i < conditionGroupList.size(); i++) {
                            ConditionGroupVo conditionGroupVo = JSON.toJavaObject(conditionGroupList.getJSONObject(i), ConditionGroupVo.class);
                            if (i > 0 && CollectionUtils.isNotEmpty(conditionGroupRelList)) {
                                if (conditionGroupRelList.size() >= i) {
                                    String joinType = conditionGroupRelList.getString(i - 1);
                                    script.append(joinType.equals("and") ? " && " : " || ");
                                } else {
                                    //数据异常跳出
                                    break;
                                }
                            }
                            script.append("(").append(conditionGroupVo.buildScript()).append(")");
                            if (CollectionUtils.isNotEmpty(conditionGroupVo.getConditionList())) {
                                for (ConditionVo conditionVo : conditionGroupVo.getConditionList()) {
                                    conditionObj.put(conditionVo.getUuid(), conditionVo.getValueList());
                                }
                            }
                        }
                        //将配置项参数处理成指定格式，格式和表达式相关，不能随意修改格式
                        JSONObject paramObj = new JSONObject();
                        JSONObject dataObj = new JSONObject();
                        JSONObject defineObj = new JSONObject();

                        List<AlertAttrDefineVo> constAttrList = AlertAttr.getConstAttrList();
                        JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
                        for (AlertAttrDefineVo constAttrVo : constAttrList) {
                            defineObj.put(constAttrVo.getName(), constAttrVo.getLabel());
                            dataObj.put(constAttrVo.getName(), new JSONArray() {{
                                this.add(alertObj.get(constAttrVo.getName().replace("const_", "")));
                            }});
                        }
                        JSONObject attrObj = alertObj.getJSONObject("attrObj");
                        if (MapUtils.isNotEmpty(attrObj)) {
                            List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
                            for (AlertAttrTypeVo attrTypeVo : attrTypeList) {
                                defineObj.put("attr_" + attrTypeVo.getName(), attrTypeVo.getLabel());
                                dataObj.put("attr_" + attrTypeVo.getName(), attrObj.getJSONArray(attrTypeVo.getName()));
                            }
                        }


                        paramObj.put("define", defineObj);
                        paramObj.put("data", dataObj);
                        paramObj.put("condition", conditionObj);
                        List<ApiRuntimeException> errorList = new ArrayList<>();
                        try {
                            isValid = JavascriptUtil.runExpression(paramObj, script.toString(), errorList);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                            errorList.add(new ApiRuntimeException(ex.getMessage()));
                            isValid = false;
                        }
                    }
                }
                if (isValid) {
                    resultObj.put("result", true);
                    IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                    AlertEventHandlerVo subHandler = alertEventMapper.getAlertEventHandlerByUuid(handlerObj.getString("uuid"));
                    if (subHandler != null) {
                        alertVo = eventHandler.trigger(subHandler, alertVo, alertEventHandlerAuditVo);
                    }
                } else {
                    resultObj.put("result", false);
                }

                resultConditionList.add(resultObj);
            }
            JSONObject result = new JSONObject();
            result.put("conditionList", resultConditionList);
            alertEventHandlerAuditVo.setResult(result);
        }
        return alertVo;
    }

    @Override
    public String getName() {
        return "CONDITION";
    }

    @Override
    public String getLabel() {
        return "条件分支";
    }

    @Override
    public String getIcon() {
        return "tsfont-accessendpoint";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_DELETE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
        }};
    }

    @Override
    public void makeupChildHandler(AlertEventHandlerVo alertEventHandlerVo) {
        if (MapUtils.isNotEmpty(alertEventHandlerVo.getConfig())) {
            JSONArray eventConditionList = alertEventHandlerVo.getConfig().getJSONArray("conditionList");
            for (int e = 0; e < eventConditionList.size(); e++) {
                JSONObject eventConditionObj = eventConditionList.getJSONObject(e);
                JSONObject handlerObj = eventConditionObj.getJSONObject("handler");
                IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                AlertEventHandlerVo subAlertEventHandlerVo = new AlertEventHandlerVo();

                subAlertEventHandlerVo.setParentId(alertEventHandlerVo.getId());
                subAlertEventHandlerVo.setEvent(alertEventHandlerVo.getEvent());
                subAlertEventHandlerVo.setAlertType(alertEventHandlerVo.getAlertType());
                subAlertEventHandlerVo.setIsActive(alertEventHandlerVo.getIsActive());
                subAlertEventHandlerVo.setUuid(handlerObj.getString("uuid"));
                subAlertEventHandlerVo.setName(handlerObj.getString("name"));
                subAlertEventHandlerVo.setHandler(handlerObj.getString("handler"));
                subAlertEventHandlerVo.setConfig(handlerObj.getJSONObject("config"));
                alertEventHandlerVo.addHandler(subAlertEventHandlerVo);
                eventHandler.makeupChildHandler(subAlertEventHandlerVo);
            }
        }
    }

    @Override
    public List<AlertEventHandlerConfigVo> getHandlerConfig(AlertEventHandlerVo alertEventHandlerVo) {
        List<AlertEventHandlerConfigVo> configList = new ArrayList<>();
        if (MapUtils.isNotEmpty(alertEventHandlerVo.getConfig())) {
            JSONArray eventConditionList = alertEventHandlerVo.getConfig().getJSONArray("conditionList");
            for (int e = 0; e < eventConditionList.size(); e++) {
                JSONObject eventConditionObj = eventConditionList.getJSONObject(e);
                JSONObject handlerObj = eventConditionObj.getJSONObject("handler");
                IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                AlertEventHandlerVo subAlertEventHandlerVo = new AlertEventHandlerVo();
                //重复使用父事件的id
                subAlertEventHandlerVo.setId(alertEventHandlerVo.getId());
                subAlertEventHandlerVo.setEvent(alertEventHandlerVo.getEvent());
                subAlertEventHandlerVo.setUuid(handlerObj.getString("uuid"));
                subAlertEventHandlerVo.setName(handlerObj.getString("name"));
                subAlertEventHandlerVo.setHandler(handlerObj.getString("handler"));
                subAlertEventHandlerVo.setConfig(handlerObj.getJSONObject("config"));
                configList.addAll(eventHandler.getHandlerConfig(subAlertEventHandlerVo));
            }
        }
        return configList;
    }

}
