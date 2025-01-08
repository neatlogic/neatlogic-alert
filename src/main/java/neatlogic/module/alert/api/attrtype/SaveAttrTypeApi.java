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
import neatlogic.framework.alert.auth.ALERT_TYPE_MODIFY;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.exception.attrtype.AlertAttrTypeNameIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_TYPE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAttrTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Override
    public String getToken() {
        return "alert/attrtype/save";
    }

    @Override
    public String getName() {
        return "保存告警扩展属性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "id,不提供代表添加"),
            @Param(name = "name", isRequired = true, desc = "唯一标识", type = ApiParamType.STRING),
            @Param(name = "label", isRequired = true, desc = "名称", type = ApiParamType.STRING),
            @Param(name = "isActive", isRequired = true, rule = "0,1", desc = "是否激活", type = ApiParamType.INTEGER),
            @Param(name = "type", desc = "类型", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT)
    })
    @Output({
            @Param(name = "id", type = ApiParamType.LONG, desc = "类型id")
    })
    @Description(desc = "保存告警扩展属性")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertAttrTypeVo alertAttrTypeVo = JSON.toJavaObject(jsonObj, AlertAttrTypeVo.class);
        if (alertAttrTypeMapper.checkAttrTypeNameIsExists(alertAttrTypeVo) > 0) {
            throw new AlertAttrTypeNameIsExistsException(alertAttrTypeVo.getName());
        }
        alertAttrTypeMapper.saveAlertAttrType(alertAttrTypeVo);
        return alertAttrTypeVo.getId();
    }

}
