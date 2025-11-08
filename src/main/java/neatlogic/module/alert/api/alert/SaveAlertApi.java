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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.queue.OriginalAlertManager;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.CREATE)
public class SaveAlertApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "alert/save";
    }

    @Override
    public String getName() {
        return "保存告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "type", desc = "告警类型", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "adaptor", desc = "转换器", type = ApiParamType.STRING),
            @Param(name = "content", desc = "告警内容", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "time", desc = "告警时间，不提供自动生成", type = ApiParamType.LONG),
    })
    @Description(desc = "保存告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        OriginalAlertVo alertVo = JSON.toJavaObject(jsonObj, OriginalAlertVo.class);
        alertVo.setSource(InputFrom.RESTFUL.getValue());
        if (alertVo.getTime() == null) {
            alertVo.setTime(new Date());
        }
        OriginalAlertManager.addAlert(alertVo);
        return null;
    }


}
