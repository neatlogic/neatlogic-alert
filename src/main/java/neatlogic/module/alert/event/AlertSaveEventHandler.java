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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertRuleMapper;
import neatlogic.module.alert.service.IAlertService;
import neatlogic.module.alert.utils.AlertRuleUtils;
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
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }
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
            if (StringUtils.isNotBlank(key)) {
                alertVo.setUniqueKey(Md5Util.encryptMD5(key));
            }
        }
        //如果uniqueKey
        if (StringUtils.isNotBlank(alertVo.getUniqueKey())) {
            if (!Md5Util.isMd5(alertVo.getUniqueKey())) {
                alertVo.setUniqueKey(Md5Util.encryptMD5(alertVo.getUniqueKey()));
            }
        }
        if (StringUtils.isNotBlank(config.getString("defaultStatus"))) {
            alertVo.setStatus(config.getString("defaultStatus"));
        }
        JSONObject resultObj = new JSONObject();
        try {
            alertService.saveAlert(alertVo);
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
        return "创建告警";
    }

    @Override
    public String getIcon() {
        return "tsfont-plus-o";
    }

    @Override
    public String getDescription() {
        return "保存告警到数据库，如果有唯一键相同的未关闭告警，将自动挂靠作为其子告警。";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
        }};
    }

    public static void main(String[] ar) {

    }

}
