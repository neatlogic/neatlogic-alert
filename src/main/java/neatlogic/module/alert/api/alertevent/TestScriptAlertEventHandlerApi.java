/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
