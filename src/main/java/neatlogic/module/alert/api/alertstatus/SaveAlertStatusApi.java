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

package neatlogic.module.alert.api.alertstatus;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_STATUS_MODIFY;
import neatlogic.framework.alert.dto.AlertStatusVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertStatusMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_STATUS_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertStatusApi extends PrivateApiComponentBase {

    @Resource
    private AlertStatusMapper alertStatusMapper;

    @Override
    public String getToken() {
        return "/alert/status/save";
    }

    @Override
    public String getName() {
        return "保存告警状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "name", desc = "唯一标识", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "label", desc = "名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "color", desc = "颜色", type = ApiParamType.STRING)
    })
    @Description(desc = "保存告警状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertStatusVo alertStatusVo = JSON.toJavaObject(jsonObj, AlertStatusVo.class);
        AlertStatusVo oldStatus = alertStatusMapper.getStatusByName(alertStatusVo.getName());
        if (oldStatus == null) {
            Integer count = alertStatusMapper.getAlertStatusCount();
            if (count == null) {
                count = 0;
            } else {
                count += 1;
            }
            alertStatusVo.setSort(count);
            alertStatusMapper.insertAlertStatus(alertStatusVo);
        } else {
            alertStatusMapper.updateAlertStatus(alertStatusVo);
        }
        return null;
    }

}
