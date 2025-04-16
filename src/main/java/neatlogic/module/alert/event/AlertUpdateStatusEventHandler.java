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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class AlertUpdateStatusEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertAuditMapper alertAuditMapper;

    @Resource
    private AlertMapper alertMapper;

    @Override
    public int getSort() {
        return 9;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }
        String status = config.getString("status");
        if (StringUtils.isNotBlank(status)) {
            if (!Objects.equals(alertVo.getStatus(), status)) {
                String oldStatus = alertVo.getStatus();
                alertVo.setStatus(status);
                alertMapper.updateAlertStatus(alertVo);

                AlertAuditVo alertAuditVo = new AlertAuditVo(true);
                alertAuditVo.setAlertId(alertVo.getId());
                alertAuditVo.setAttrName("const_status");
                alertAuditVo.addOldValue(oldStatus);
                alertAuditVo.addNewValue(alertVo.getStatus());
                alertAuditMapper.insertAlertAudit(alertAuditVo);

                AlertEventManager.doEvent(AlertEventType.ALERT_STATUE_CHANGE, alertVo);

                JSONObject resultObj = new JSONObject();
                resultObj.put("oldStatus", oldStatus);
                resultObj.put("newStatus", alertVo.getStatus());
                alertEventHandlerAuditVo.setResult(resultObj);
            }
        }
        return alertVo;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public String getName() {
        return "UPDATESTATUS";
    }

    @Override
    public String getLabel() {
        return "修改状态";
    }

    @Override
    public String getIcon() {
        return "tsfont-heart-s";
    }

    @Override
    public String getDescription() {
        return "修改告警状态，如果告警状态已经修改到位，则不会重复触发。";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
        }};
    }

}
