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
