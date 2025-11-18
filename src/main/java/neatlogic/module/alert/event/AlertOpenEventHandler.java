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
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class AlertOpenEventHandler extends AlertEventHandlerBase {
    @Resource
    private IAlertService alertService;

    @Resource
    private AlertMapper alertMapper;

    @Override
    public int getSort() {
        return 7;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }
        String openType = config.getString("openType");
        int isCloseChildAlert = config.getIntValue("isCloseChildAlert");
        if (StringUtils.isBlank(openType)) {
            openType = "id";
        }

        JSONObject resultObj = new JSONObject();
        if (Objects.equals(openType, "id")) {
            try {
                alertVo.setIsCloseChildAlert(isCloseChildAlert);
                alertService.closeAlert(alertVo);
                resultObj.put("openCount", 1);
            } catch (Exception e) {
                throw new AlertEventHandlerTriggerException(e);
            }
        } else if (Objects.equals(openType, "uniquekey")) {
            if (CollectionUtils.isNotEmpty(config.getJSONArray("uniqueAttrList"))) {
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
                        key += alertObj.getString(attr.substring("const_".length()));
                    } else if (attr.startsWith("attr_")) {
                        JSONObject attrObj = alertObj.getJSONObject("attrObj");
                        if (attrObj != null && attrObj.get(attr.substring("attr_".length())) != null) {
                            if (StringUtils.isNotBlank(key)) {
                                key += "#";
                            }
                            key += attrObj.getString(attr.substring("attr_".length()));
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
                List<AlertVo> alertList = alertMapper.getCloseAlertByUniqueKey(alertVo.getUniqueKey());
                if (CollectionUtils.isNotEmpty(alertList)) {
                    for (AlertVo alert : alertList) {
                        alert.setIsCloseChildAlert(isCloseChildAlert);
                        alertService.openAlert(alert);
                    }
                }
                resultObj.put("openCount", alertList.size());
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
        return "OPEN";
    }

    @Override
    public String getLabel() {
        return "打开告警";
    }

    @Override
    public String getIcon() {
        return "tsfont-check-o";
    }

    @Override
    public String getDescription() {
        return "重新打开已经关闭的告警，如果告警已经处于打开状态不会触发。";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<>() {{
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
        }};
    }

}
