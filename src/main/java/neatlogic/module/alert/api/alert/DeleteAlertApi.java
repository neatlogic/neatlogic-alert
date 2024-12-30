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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.service.IAlertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteAlertApi extends PrivateApiComponentBase {

    @Resource
    private IAlertService alertService;

    @Override
    public String getToken() {
        return "alert/delete";
    }

    @Override
    public String getName() {
        return "删除告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "isDeleteChildAlert", isRequired = true, rule = "0,1", desc = "是否删除子告警", type = ApiParamType.INTEGER)
    })
    @Description(desc = "删除告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long alertId = jsonObj.getLong("id");
        Integer isDeleteChildAlert = jsonObj.getInteger("isDeleteChildAlert");
        alertService.deleteAlert(alertId, Objects.equals(1, isDeleteChildAlert));
        return null;
    }


}
