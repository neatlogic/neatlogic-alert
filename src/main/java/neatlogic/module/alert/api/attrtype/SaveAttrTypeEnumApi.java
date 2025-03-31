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

package neatlogic.module.alert.api.attrtype;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ATTR_MODIFY;
import neatlogic.framework.alert.dto.AlertAttrTypeEnumVo;
import neatlogic.framework.alert.exception.attrtype.AlertAttrTypeEnumIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_ATTR_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAttrTypeEnumApi extends PrivateApiComponentBase {

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Override
    public String getToken() {
        return "alert/attrenum/save";
    }

    @Override
    public String getName() {
        return "保存告警扩展属性成员";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "value", desc = "值", type = ApiParamType.STRING, isRequired = true, maxLength = 50),
            @Param(name = "text", desc = "文案", type = ApiParamType.STRING, isRequired = true, maxLength = 100),
            @Param(name = "attrType", isRequired = true, desc = "属性类型", type = ApiParamType.LONG)
    })
    @Output({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG)
    })
    @Description(desc = "保存告警扩展属性成员")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertAttrTypeEnumVo alertAttrTypeEnumVo = JSON.toJavaObject(jsonObj, AlertAttrTypeEnumVo.class);
        if (alertAttrTypeMapper.checkAttrTypeEnumValueIsExists(alertAttrTypeEnumVo) > 0) {
            throw new AlertAttrTypeEnumIsExistsException(alertAttrTypeEnumVo.getValue());
        }
        alertAttrTypeMapper.saveAlertAttrTypeEnum(alertAttrTypeEnumVo);
        alertAttrTypeEnumVo = alertAttrTypeMapper.getAttrTypeEnumByValue(alertAttrTypeEnumVo);
        return alertAttrTypeEnumVo.getId();
    }

}
