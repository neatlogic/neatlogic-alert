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
import neatlogic.framework.util.UuidUtil;
import neatlogic.module.alert.dao.mapper.AlertRuleMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AlertSaveEventHandler extends AlertEventHandlerBase {
    @Resource
    private IAlertService alertService;

    @Resource
    private AlertRuleMapper alertRuleMapper;


    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }
        String uniqueKeyOriginal = null;
        //根据唯一规则计算unique key
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
                uniqueKeyOriginal = key;
                if (!Md5Util.isMd5(key)) {
                    alertVo.setUniqueKey(Md5Util.encryptMD5(key));
                } else {
                    alertVo.setUniqueKey(key);
                }
            }
        }
        //如果uniqueKey
        if (StringUtils.isNotBlank(alertVo.getUniqueKey())) {
            if (StringUtils.isBlank(uniqueKeyOriginal)) {
                uniqueKeyOriginal = alertVo.getUniqueKey();
            }
            if (!Md5Util.isMd5(alertVo.getUniqueKey())) {
                alertVo.setUniqueKey(Md5Util.encryptMD5(alertVo.getUniqueKey()));
            }
        } else {
            //如果没有uniquekey则随机生成一个
            uniqueKeyOriginal = UuidUtil.randomUuid();
            alertVo.setUniqueKey(uniqueKeyOriginal);
        }
        alertEventHandlerAuditVo.setUniqueKey(alertVo.getUniqueKey());
        if (StringUtils.isNotBlank(config.getString("defaultStatus"))) {
            alertVo.setStatus(config.getString("defaultStatus"));
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("uniqueKeyOriginal", uniqueKeyOriginal);
        resultObj.put("uniqueKey", alertVo.getUniqueKey());
        alertEventHandlerAuditVo.setResult(resultObj);
        try {
            alertService.saveAlert(alertVo, config.getBooleanValue("serialSave"));
            resultObj.put("alertId", alertVo.getId());
            resultObj.put("alertTitle", alertVo.getTitle());
            if (alertVo.getFromAlertVo() != null) {
                resultObj.put("fromAlertId", alertVo.getFromAlertVo().getId());
                resultObj.put("fromAlertTitle", alertVo.getFromAlertVo().getTitle());
            }
            alertEventHandlerAuditVo.setResult(resultObj);
        } catch (Exception e) {
            throw new AlertEventHandlerTriggerException(e);
        }
        return alertVo;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public int getSort() {
        return 1;
    }

    @Override
    public String getName() {
        return "SAVE";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.savehandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-plus-o";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.savehandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
            this.add("ai_agent");
        }};
    }


}
