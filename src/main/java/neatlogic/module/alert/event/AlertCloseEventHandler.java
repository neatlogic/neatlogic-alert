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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.alert.utils.AlertRuleUtils;
import neatlogic.framework.util.$;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertRuleMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AlertCloseEventHandler extends AlertEventHandlerBase {
    @Resource
    private IAlertService alertService;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertRuleMapper alertRuleMapper;

    @Override
    public int getSort() {
        return 6;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }
        String closeType = config.getString("closeType");
        int isCloseChildAlert = config.getIntValue("isCloseChildAlert");
        if (StringUtils.isBlank(closeType)) {
            closeType = "id";
        }

        JSONObject resultObj = new JSONObject();
        if (Objects.equals(closeType, "id")) {
            try {
                alertVo.setIsCloseChildAlert(isCloseChildAlert);
                alertService.closeAlert(alertVo);
                resultObj.put("closeCount", 1);
            } catch (Exception e) {
                throw new AlertEventHandlerTriggerException(e);
            }
        } else if (Objects.equals(closeType, "uniquekey")) {
            if (CollectionUtils.isNotEmpty(config.getJSONArray("uniqueAttrList"))) {
                List<AlertRuleVo> ruleList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(config.getJSONArray("ruleList"))) {
                    List<Long> ruleIdList = new ArrayList<>();
                    for (int i = 0; i < config.getJSONArray("ruleList").size(); i++) {
                        ruleIdList.add(config.getJSONArray("ruleList").getLong(i));
                    }
                    ruleList = alertRuleMapper.getAlertRuleByIdList(ruleIdList);
                }

                List<String> attrList = new ArrayList<>();
                for (int i = 0; i < config.getJSONArray("uniqueAttrList").size(); i++) {
                    attrList.add(config.getJSONArray("uniqueAttrList").getJSONObject(i).getString("name"));
                }
                //按属性名排序，避免由于顺序不同导致结果不同
                attrList.sort(String::compareTo);
                String key = "";
                JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
                for (String attr : attrList) {
                    if (attr.startsWith("const_")) {
                        if (StringUtils.isNotBlank(key)) {
                            key += "#";
                        }
                        String value = alertObj.getString(attr.substring("const_".length()));
                        List<AlertRuleVo> tmpRuleList = ruleList.stream().filter(d -> d.getAttrName().equals(attr)).collect(Collectors.toList());
                        if (CollectionUtils.isNotEmpty(tmpRuleList)) {
                            value = AlertRuleUtils.doRule(value, tmpRuleList);
                        }
                        key += value;
                    } else if (attr.startsWith("attr_")) {
                        JSONObject attrObj = alertObj.getJSONObject("attrObj");
                        if (attrObj != null && attrObj.get(attr.substring("attr_".length())) != null) {
                            if (StringUtils.isNotBlank(key)) {
                                key += "#";
                            }
                            String value = attrObj.getString(attr.substring("attr_".length()));
                            List<AlertRuleVo> tmpRuleList = ruleList.stream().filter(d -> d.getAttrName().equals(attr)).collect(Collectors.toList());
                            if (CollectionUtils.isNotEmpty(tmpRuleList)) {
                                value = AlertRuleUtils.doRule(value, tmpRuleList);
                            }
                            key += value;
                        }
                    }
                }
                //一定要判断，因为可能直接选唯一键作为唯一键，这时候就需要二次转换
                if (StringUtils.isNotBlank(key)) {
                    if (!Md5Util.isMd5(key)) {
                        alertVo.setUniqueKey(Md5Util.encryptMD5(key));
                    } else {
                        alertVo.setUniqueKey(key);
                    }
                }
            }
            try {
                List<AlertVo> alertList = alertMapper.getOpenAlertByUniqueKey(alertVo.getUniqueKey());
                if (CollectionUtils.isNotEmpty(alertList)) {
                    for (AlertVo alert : alertList) {
                        alert.setIsCloseChildAlert(isCloseChildAlert);
                        alertService.closeAlert(alert);
                    }
                }
                resultObj.put("closeCount", alertList.size());
            } catch (Exception e) {
                throw new AlertEventHandlerTriggerException(e);
            }
        }
        alertEventHandlerAuditVo.setResult(resultObj);
        return alertVo;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public String getName() {
        return "CLOSE";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.closehandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-close-o";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.closehandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
        }};
    }

}
