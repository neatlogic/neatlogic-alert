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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.adaptor.core.AlertAdaptorManager;
import neatlogic.framework.alert.auth.ALERT_TYPE_MODIFY;
import neatlogic.framework.alert.dto.AlertTypeVo;
import neatlogic.framework.alert.exception.alerttype.AlertTypeIsExistsException;
import neatlogic.framework.alert.exception.alerttype.AlertTypeNotFoundException;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_TYPE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertTypeMapper alertTypeMapper;

    @Override
    public String getToken() {
        return "alert/alerttype/save";
    }

    @Override
    public String getName() {
        return "保存告警类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "唯一标识", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "label", desc = "名称", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.INTEGER),
            @Param(name = "fileId", desc = "插件附件id", type = ApiParamType.LONG)
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id")
    })
    @Description(desc = "保存告警类型")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        Long id = jsonObj.getLong("id");
        AlertTypeVo alertTypeVo = JSON.toJavaObject(jsonObj, AlertTypeVo.class);
        if (alertTypeMapper.checkAlertTypeNameIsExists(alertTypeVo) > 0) {
            throw new AlertTypeIsExistsException(alertTypeVo.getName());
        }
        if (id == null) {
            alertTypeVo.setFcu(UserContext.get().getUserUuid(true));
            alertTypeMapper.insertAlertType(alertTypeVo);
        } else {
            AlertTypeVo oldAlertVo = alertTypeMapper.getAlertTypeById(id);
            if (oldAlertVo == null) {
                throw new AlertTypeNotFoundException(id);
            }
            alertTypeVo.setLcu(UserContext.get().getUserUuid(true));
            alertTypeMapper.updateAlertType(alertTypeVo);
            //清除适配器缓存
            AlertAdaptorManager.removeAdapter(oldAlertVo.getName());
        }
        return alertTypeVo.getId();
    }

}
