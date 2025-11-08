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

package neatlogic.module.alert.api.alertevent;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertEventVo;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertEventApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "alert/event/list";
    }

    @Override
    public String getName() {
        return "列出所有告警事件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = AlertEventVo[].class)})
    @Description(desc = "列出所有告警事件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AlertEventVo> eventList = new ArrayList<>();
        for (AlertEventType event : AlertEventType.values()) {
            AlertEventVo alertEventVo = new AlertEventVo();
            alertEventVo.setName(event.getName());
            alertEventVo.setLabel(event.getLabel());
            alertEventVo.setDescription(event.getDescription());
            eventList.add(alertEventVo);
        }
        return eventList;
    }


}
