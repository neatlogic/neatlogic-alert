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

package neatlogic.module.alert.api.alertnotifytemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_NOTIFY_TEMPLATE_MODIFY;
import neatlogic.framework.alert.dto.AlertNotifyTemplateVo;
import neatlogic.framework.alert.exception.alertnotifytemplate.NotifyTemplateNameIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertNotifyTemplateMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_NOTIFY_TEMPLATE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAlertNotifyTemplateApi extends PrivateApiComponentBase {

    @Resource
    private AlertNotifyTemplateMapper alertNotifyTemplateMapper;

    @Override
    public String getToken() {
        return "alert/notifytemplate/save";
    }

    @Override
    public String getName() {
        return "保存通知模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "唯一标识", type = ApiParamType.STRING, isRequired = true, maxLength = 50),
            @Param(name = "label", desc = "名称", type = ApiParamType.STRING, isRequired = true, maxLength = 50),
            @Param(name = "title", desc = "标题", type = ApiParamType.STRING),
            @Param(name = "content", desc = "内容", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "isActive", desc = "是否激活", rule = "0,1", type = ApiParamType.INTEGER, isRequired = true),
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "模板id")
    })
    @Description(desc = "保存通知模板")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertNotifyTemplateVo alertNotifyTemplateVo = JSON.toJavaObject(jsonObj, AlertNotifyTemplateVo.class);
        if (alertNotifyTemplateMapper.checkNotifyTemplateNameIsExists(alertNotifyTemplateVo) > 0) {
            throw new NotifyTemplateNameIsExistsException(alertNotifyTemplateVo.getName());
        }
        Long id = jsonObj.getLong("id");
        if (id == null) {
            alertNotifyTemplateMapper.insertNotifyTemplate(alertNotifyTemplateVo);
        } else {
            alertNotifyTemplateMapper.updateNotifyTemplate(alertNotifyTemplateVo);
        }
        return alertNotifyTemplateVo.getId();
    }

}
