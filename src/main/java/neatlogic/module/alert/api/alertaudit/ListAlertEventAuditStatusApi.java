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

package neatlogic.module.alert.api.alertaudit;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.enums.AlertEventStatus;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertEventAuditStatusApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/alert/event/audit/status/list";
    }

    @Override
    public String getName() {
        return "获取告警事件执行状态列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "获取告警事件执行状态列表")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        List<ValueTextVo> statusList = new ArrayList<>();
        for (AlertEventStatus status : AlertEventStatus.values()) {
            statusList.add(new ValueTextVo(status.getValue(), status.getText()));
        }
        return statusList;
    }
}
