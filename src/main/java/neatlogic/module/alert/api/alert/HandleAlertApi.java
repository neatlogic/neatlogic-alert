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
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.service.IAlertService;
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
            @Param(name = "id", desc = "id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "status", desc = "状态", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "comment", desc = "评论", type = ApiParamType.STRING),
            @Param(name = "isChangeChildAlertStatus", desc = "是否更新子告警状态", type = ApiParamType.INTEGER)
    })
    @Output({@Param(name = "id", desc = "告警id", type = ApiParamType.LONG)})
    @Description(desc = "处理告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertVo alertVo = JSON.toJavaObject(jsonObj, AlertVo.class);
        alertService.handleAlert(alertVo);
        return alertVo.getId();
    }


}
