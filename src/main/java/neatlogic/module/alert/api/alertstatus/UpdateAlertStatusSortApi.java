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

import com.alibaba.fastjson.JSONArray;
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
public class UpdateAlertStatusSortApi extends PrivateApiComponentBase {

    @Resource
    private AlertStatusMapper alertStatusMapper;

    @Override
    public String getToken() {
        return "/alert/status/sort/update";
    }

    @Override
    public String getName() {
        return "更新告警状态排序";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "statusList", desc = "状态列表", type = ApiParamType.JSONARRAY, isRequired = true)})
    @Description(desc = "更新告警状态排序")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        JSONArray statusList = jsonObj.getJSONArray("statusList");
        for (int i = 0; i < statusList.size(); i++) {
            AlertStatusVo alertStatusVo = new AlertStatusVo();
            alertStatusVo.setSort(i + 1);
            alertStatusVo.setName(statusList.getString(i));
            alertStatusMapper.updateAlertStatusSort(alertStatusVo);
        }
        return null;
    }

}
