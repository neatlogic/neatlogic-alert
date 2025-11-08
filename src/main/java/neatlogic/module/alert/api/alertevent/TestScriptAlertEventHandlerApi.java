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
import neatlogic.framework.alert.auth.ALERT_EVENT_MODIFY;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.javascript.JavascriptUtil;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = ALERT_EVENT_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class TestScriptAlertEventHandlerApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "alert/event/handler/script/test";
    }

    @Override
    public String getName() {
        return "测试脚本事件插件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "script", desc = "脚本", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "alertData", desc = "告警参数", isRequired = true, type = ApiParamType.JSONOBJECT)
    })
    @Output({@Param(name = "result", desc = "转换结果", type = ApiParamType.STRING)})
    @Description(desc = "测试脚本事件插件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject alertData = jsonObj.getJSONObject("alertData");
        String script = jsonObj.getString("script");
        JSONObject returnObj = new JSONObject();
        try {
            returnObj.put("result", JavascriptUtil.transform(alertData, script));
        } catch (Exception e) {
            returnObj.put("error", e.getMessage());
        }
        return returnObj;
    }
}
