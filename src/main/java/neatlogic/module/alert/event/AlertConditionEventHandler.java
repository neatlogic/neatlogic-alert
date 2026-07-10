/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.dto.condition.ConditionGroupVo;
import neatlogic.framework.alert.dto.condition.ConditionVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.enums.AlertEventStatus;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.event.IAlertEventHandler;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.util.$;
import neatlogic.framework.util.javascript.JavascriptResult;
import neatlogic.framework.util.javascript.JavascriptUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class AlertConditionEventHandler extends AlertEventHandlerBase {
    private final Logger logger = LoggerFactory.getLogger(AlertConditionEventHandler.class);
    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;


    @Override
    public int getSort() {
        return 4;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) {
        if (MapUtils.isNotEmpty(alertEventHandlerVo.getConfig())) {
            JSONArray eventConditionList = alertEventHandlerVo.getConfig().getJSONArray("conditionList");
            JSONArray resultConditionList = new JSONArray();
            for (int e = 0; e < eventConditionList.size(); e++) {
                JSONObject resultObj = new JSONObject();
                JSONObject eventConditionObj = eventConditionList.getJSONObject(e);
                Object handlerObject = eventConditionObj.get("handler");
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

                        List<AlertAttrDefineVo> constAttrList = AlertAttr.getConditionConstAttrList();
                        JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
                        for (AlertAttrDefineVo constAttrVo : constAttrList) {
                            defineObj.put(constAttrVo.getName(), constAttrVo.getLabel());
                            dataObj.put(constAttrVo.getName(), new JSONArray() {{
                                Object o = alertObj.get(constAttrVo.getName().replace("const_", ""));
                                if (o != null) {
                                    this.add(o);
                                }
                            }});
                        }
                        JSONObject attrObj = alertObj.getJSONObject("attrObj");
                        if (MapUtils.isNotEmpty(attrObj)) {
                            List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
                            for (AlertAttrTypeVo attrTypeVo : attrTypeList) {
                                defineObj.put("attr_" + attrTypeVo.getName(), attrTypeVo.getLabel());
                                dataObj.put("attr_" + attrTypeVo.getName(),
                                        new JSONArray() {{
                                            this.add(attrObj.get(attrTypeVo.getName()));
                                        }}
                                );
                            }
                        }


                        paramObj.put("define", defineObj);
                        paramObj.put("data", dataObj);
                        paramObj.put("condition", conditionObj);
                        Map<String, JavascriptResult> resultMap = new HashMap<>();
                        resultObj.put("resultMap", resultMap);
                        try {
                            isValid = JavascriptUtil.runExpression(paramObj, script.toString(), resultMap);
                        } catch (Exception ex) {
                            logger.warn(ex.getMessage(), ex);
                            resultObj.put("error", ex.getMessage());
                            isValid = false;
                        }
                    }
                }
                if (isValid) {
                    resultObj.put("result", true);
                    InputFromContext.init(InputFrom.EVENT);
                    if (handlerObject instanceof JSONObject) {
                        JSONObject handlerObj = (JSONObject) handlerObject;
                        IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                        AlertEventHandlerVo subHandler = alertEventMapper.getAlertEventHandlerByUuid(handlerObj.getString("uuid"));
                        if (subHandler != null) {
                            alertVo = eventHandler.trigger(subHandler, alertVo, alertEventHandlerAuditVo.getId());
                        }
                    } else if (handlerObject instanceof JSONArray) {
                        for (int i = 0; i < ((JSONArray) handlerObject).size(); i++) {
                            JSONObject handlerObj = ((JSONArray) handlerObject).getJSONObject(i);
                            IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                            AlertEventHandlerVo subHandler = alertEventMapper.getAlertEventHandlerByUuid(handlerObj.getString("uuid"));
                            if (subHandler != null) {
                                alertVo = eventHandler.trigger(subHandler, alertVo, alertEventHandlerAuditVo.getId());
                            }
                        }
                    }
                } else {
                    resultObj.put("result", false);
                    //修改审计状态
                    alertEventHandlerAuditVo.setStatus(AlertEventStatus.FAILED.getValue());
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
    public boolean isAsync() {
        return false;
    }

    @Override
    public String getName() {
        return "CONDITION";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.conditionhandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-accessendpoint";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.conditionhandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_DELETE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<>() {{
            this.add("interval");
            this.add("integration");
            this.add("similar");
            this.add("ai_agent");
        }};
    }

    @Override
    public void makeupChildHandler(AlertEventHandlerVo alertEventHandlerVo) {
        if (MapUtils.isNotEmpty(alertEventHandlerVo.getConfig())) {
            JSONArray eventConditionList = alertEventHandlerVo.getConfig().getJSONArray("conditionList");
            for (int e = 0; e < eventConditionList.size(); e++) {
                JSONObject eventConditionObj = eventConditionList.getJSONObject(e);
                Object handler = eventConditionObj.get("handler");
                if (handler instanceof JSONObject) {
                    JSONObject handlerObj = (JSONObject) handler;
                    IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                    AlertEventHandlerVo subAlertEventHandlerVo = new AlertEventHandlerVo();

                    subAlertEventHandlerVo.setParentId(alertEventHandlerVo.getId());
                    subAlertEventHandlerVo.setEvent(alertEventHandlerVo.getEvent());
                    subAlertEventHandlerVo.setAlertType(alertEventHandlerVo.getAlertType());
                    subAlertEventHandlerVo.setIsActive(handlerObj.getIntValue("isActive"));
                    subAlertEventHandlerVo.setTypeId(handlerObj.getLong("typeId"));
                    subAlertEventHandlerVo.setIsAsync(handlerObj.getIntValue("isAsync"));
                    subAlertEventHandlerVo.setUuid(handlerObj.getString("uuid"));
                    subAlertEventHandlerVo.setName(handlerObj.getString("name"));
                    subAlertEventHandlerVo.setHandler(handlerObj.getString("handler"));
                    subAlertEventHandlerVo.setConfig(handlerObj.getJSONObject("config"));
                    alertEventHandlerVo.addHandler(subAlertEventHandlerVo);
                    eventHandler.makeupChildHandler(subAlertEventHandlerVo);
                } else if (handler instanceof JSONArray) {
                    for (int h = 0; h < eventConditionObj.getJSONArray("handler").size(); h++) {
                        JSONObject handlerObj = eventConditionObj.getJSONArray("handler").getJSONObject(h);
                        IAlertEventHandler eventHandler = AlertEventHandlerFactory.getHandler(handlerObj.getString("handler"));
                        AlertEventHandlerVo subAlertEventHandlerVo = new AlertEventHandlerVo();

                        subAlertEventHandlerVo.setParentId(alertEventHandlerVo.getId());
                        subAlertEventHandlerVo.setEvent(alertEventHandlerVo.getEvent());
                        subAlertEventHandlerVo.setAlertType(alertEventHandlerVo.getAlertType());
                        subAlertEventHandlerVo.setIsActive(handlerObj.getIntValue("isActive"));
                        subAlertEventHandlerVo.setTypeId(handlerObj.getLong("typeId"));
                        subAlertEventHandlerVo.setIsAsync(handlerObj.getIntValue("isAsync"));
                        subAlertEventHandlerVo.setUuid(handlerObj.getString("uuid"));
                        subAlertEventHandlerVo.setName(handlerObj.getString("name"));
                        subAlertEventHandlerVo.setHandler(handlerObj.getString("handler"));
                        subAlertEventHandlerVo.setConfig(handlerObj.getJSONObject("config"));
                        alertEventHandlerVo.addHandler(subAlertEventHandlerVo);
                        eventHandler.makeupChildHandler(subAlertEventHandlerVo);
                    }
                }
            }
        }
    }

    /*@Override
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
    }*/

}
