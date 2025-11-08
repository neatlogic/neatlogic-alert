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
            @Param(name = "isNormal", rule = "0,1", desc = "作为普通属性展示", type = ApiParamType.INTEGER),
            @Param(name = "isActive", isRequired = true, rule = "0,1", desc = "是否激活", type = ApiParamType.INTEGER),
            @Param(name = "isRow", rule = "0,1", desc = "是否独立一行展示", type = ApiParamType.INTEGER),
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
        Long id = jsonObj.getLong("id");
        if (alertAttrTypeVo.getIsNormal() == null) {
            alertAttrTypeVo.setIsNormal(0);
        }
        if (alertAttrTypeMapper.checkAttrTypeNameIsExists(alertAttrTypeVo) > 0) {
            throw new AlertAttrTypeNameIsExistsException(alertAttrTypeVo.getName());
        }
        if (id == null) {
            Integer c = alertAttrTypeMapper.getAttrTypeCount();
            if (c == null) {
                c = 0;
            }
            alertAttrTypeVo.setSort(c + 1);
        }
        alertAttrTypeMapper.saveAlertAttrType(alertAttrTypeVo);
        return alertAttrTypeVo.getId();
    }

}
