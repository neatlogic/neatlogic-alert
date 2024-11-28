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

package neatlogic.module.alert.api.alerttype;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.adaptor.core.AlertAdaptorManager;
import neatlogic.framework.alert.auth.ALERT_TYPE_MODIFY;
import neatlogic.framework.alert.dto.AlertTypeVo;
import neatlogic.framework.alert.exception.alerttype.AlertTypeNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_TYPE_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteAlertTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertTypeMapper alertTypeMapper;

    @Override
    public String getToken() {
        return "alert/alerttype/delete";
    }

    @Override
    public String getName() {
        return "删除告警类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
    })
    @Description(desc = "删除告警类型")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        AlertTypeVo oldAlertVo = alertTypeMapper.getAlertTypeById(id);
        if (oldAlertVo == null) {
            throw new AlertTypeNotFoundException(id);
        }
        alertTypeMapper.deleteAlertTypeById(id);
        //清除适配器缓存
        AlertAdaptorManager.removeAdapter(oldAlertVo.getName());
        return null;
    }
}
