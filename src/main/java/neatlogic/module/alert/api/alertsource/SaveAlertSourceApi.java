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

package neatlogic.module.alert.api.alertsource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_SOURCE_MODIFY;
import neatlogic.framework.alert.dto.AlertSourceVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertSourceMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_SOURCE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAlertSourceApi extends PrivateApiComponentBase {

    @Resource
    private AlertSourceMapper alertSourceMapper;

    @Override
    public String getToken() {
        return "/alert/source/save";
    }

    @Override
    public String getName() {
        return "保存告警来源";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, desc = "唯一标识", isRequired = true, maxLength = 50),
            @Param(name = "label", type = ApiParamType.STRING, desc = "名称", isRequired = true, maxLength = 50),
    })
    @Description(desc = "保存告警来源")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertSourceVo alertSourceVo = JSON.toJavaObject(jsonObj, AlertSourceVo.class);
        alertSourceMapper.saveAlertSource(alertSourceVo);
        return null;
    }
}
