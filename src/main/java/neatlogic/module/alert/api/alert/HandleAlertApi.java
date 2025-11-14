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

package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class HandleAlertApi extends PrivateApiComponentBase {

    @Resource
    private IAlertService alertService;

    @Override
    public String getToken() {
        return "alert/handle";
    }

    @Override
    public String getName() {
        return "处理告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "idList", desc = "id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "status", desc = "状态", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "isClose", desc = "是否关闭", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "comment", desc = "评论", type = ApiParamType.STRING),
            @Param(name = "isChangeChildAlertStatus", desc = "是否更新子告警状态", type = ApiParamType.INTEGER)
    })
    @Description(desc = "处理告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        JSONArray idList = jsonObj.getJSONArray("idList");
        if (id == null && CollectionUtils.isEmpty(idList)) {
            throw new ParamNotExistsException("id", "idList");
        }
        if (id != null) {
            AlertVo alertVo = JSON.toJavaObject(jsonObj, AlertVo.class);
            alertService.handleAlert(alertVo);
        } else if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                AlertVo alertVo = JSON.toJavaObject(jsonObj, AlertVo.class);
                alertVo.setId(idList.getLong(i));
                alertService.handleAlert(alertVo);
            }
        }
        return null;
    }


}
