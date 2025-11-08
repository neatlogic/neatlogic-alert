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

package neatlogic.module.alert.api.alerttype;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.adaptor.core.AlertAdaptorManager;
import neatlogic.framework.alert.auth.ALERT_TYPE_MODIFY;
import neatlogic.framework.alert.dto.AlertTypeAdaptorVo;
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
import org.apache.commons.collections4.CollectionUtils;
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
            @Param(name = "isActive", desc = "是否激活", rule = "0,1", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "fileId", desc = "插件附件id", type = ApiParamType.LONG),
            @Param(name = "attrTypeIdList", desc = "扩展属性id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "adaptorList", desc = "插件列表", type = ApiParamType.JSONARRAY),
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
            alertTypeMapper.deleteAlertTypeAttrTypeByAlertTypeId(id);
            alertTypeMapper.deleteAlertTypeAdaptorByAlertTypeId(id);
            alertTypeVo.setLcu(UserContext.get().getUserUuid(true));
            alertTypeMapper.updateAlertType(alertTypeVo);
            //清除适配器缓存
            AlertAdaptorManager.removeAdapter(oldAlertVo.getName());
        }
        if (CollectionUtils.isNotEmpty(alertTypeVo.getAttrTypeIdList())) {
            for (int i = 0; i < alertTypeVo.getAttrTypeIdList().size(); i++) {
                alertTypeMapper.insertAlertTypeAttrType(alertTypeVo.getId(), alertTypeVo.getAttrTypeIdList().get(i), i + 1);
            }
        }
        if (CollectionUtils.isNotEmpty(alertTypeVo.getAdaptorList())) {
            for (AlertTypeAdaptorVo adaptor : alertTypeVo.getAdaptorList()) {
                adaptor.setAlertTypeId(alertTypeVo.getId());
                alertTypeMapper.insertAlertTypeAdaptor(adaptor);
            }
        }
        return alertTypeVo.getId();
    }

}
