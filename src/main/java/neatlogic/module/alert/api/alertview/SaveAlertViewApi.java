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

package neatlogic.module.alert.api.alertview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertViewAuthVo;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.alert.exception.alertview.AlertViewIsExistsException;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertViewApi extends PrivateApiComponentBase {

    @Resource
    private AlertViewMapper alertViewMapper;


    @Override
    public String getToken() {
        return "alert/view/save";
    }

    @Override
    public String getName() {
        return "保存告警视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "唯一标识", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "label", desc = "名称", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "catalogId", desc = "目录id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.INTEGER),
            @Param(name = "config", desc = "配置", isRequired = true, type = ApiParamType.JSONOBJECT)
    })
    @Output({@Param(name = "id", desc = "视图id", type = ApiParamType.LONG)})
    @Description(desc = "保存告警视图")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertViewVo alertViewVo = JSON.toJavaObject(jsonObj, AlertViewVo.class);
        if (alertViewMapper.checkAlertViewIsExists(alertViewVo) > 0) {
            throw new AlertViewIsExistsException(alertViewVo.getName());
        }
        Long id = jsonObj.getLong("id");
        if (id != null) {
            alertViewVo.setLcu(UserContext.get().getUserUuid(true));
            alertViewMapper.deleteAlertViewAuthByViewId(id);
        } else {
            alertViewVo.setFcu(UserContext.get().getUserUuid(true));
        }
        //清除权限，重新从前端数据中获取
        alertViewVo.setAlertViewAuthList(null);
        alertViewMapper.saveAlertView(alertViewVo);
        if (CollectionUtils.isNotEmpty(alertViewVo.getAlertViewAuthList())) {
            for (AlertViewAuthVo authVo : alertViewVo.getAlertViewAuthList()) {
                authVo.setViewId(alertViewVo.getId());
                alertViewMapper.insertAlertViewAuth(authVo);
            }
        }
        return alertViewVo.getId();
    }


}
