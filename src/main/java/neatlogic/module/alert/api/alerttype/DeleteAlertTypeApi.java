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

package neatlogic.module.alert.api.alerttype;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.adaptor.core.AlertAdaptorManager;
import neatlogic.framework.alert.auth.ALERT_TYPE_MODIFY;
import neatlogic.framework.alert.dto.AlertTypeVo;
import neatlogic.framework.alert.exception.alerttype.AlertTypeNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_TYPE_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteAlertTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertTypeMapper alertTypeMapper;

    @Override
    public String getToken() {
        return "alert/alerttype/delete";
    }

    @Override
    public String getName() {
        return "删除告警类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
    })
    @Description(desc = "删除告警类型")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        AlertTypeVo oldAlertVo = alertTypeMapper.getAlertTypeById(id);
        if (oldAlertVo == null) {
            throw new AlertTypeNotFoundException(id);
        }
        alertTypeMapper.deleteAlertTypeById(id);
        //清除适配器缓存
        AlertAdaptorManager.removeAdapter(oldAlertVo.getName());
        return null;
    }
}
