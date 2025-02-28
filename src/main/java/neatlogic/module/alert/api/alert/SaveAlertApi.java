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
