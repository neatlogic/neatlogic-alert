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

package neatlogic.module.alert.api.alertrule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_RULE_MODIFY;
import neatlogic.framework.alert.dto.AlertRuleVo;
import neatlogic.framework.alert.exception.alertrule.AlertRuleIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertRuleMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_RULE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAlertRuleApi extends PrivateApiComponentBase {

    @Resource
    private AlertRuleMapper alertRuleMapper;

    @Override
    public String getToken() {
        return "/alert/rule/save";
    }

    @Override
    public String getName() {
        return "保存告警规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "唯一标识", isRequired = true),
            @Param(name = "label", type = ApiParamType.STRING, desc = "名称", isRequired = true),
            @Param(name = "attrName", type = ApiParamType.STRING, desc = "关联属性", isRequired = true),
            @Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活", rule = "0,1", isRequired = true),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "配置", isRequired = true),
    })
    @Output({@Param(name = "id", type = ApiParamType.LONG, desc = "id")})
    @Description(desc = "保存告警规则")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertRuleVo alertRuleVo = JSON.toJavaObject(jsonObj, AlertRuleVo.class);
        Long id = jsonObj.getLong("id");
        if (alertRuleMapper.checkAlertRuleIsExists(alertRuleVo) > 0) {
            throw new AlertRuleIsExistsException(alertRuleVo.getName());
        }
        if (id == null) {
            alertRuleMapper.insertAlertRule(alertRuleVo);
        } else {
            alertRuleMapper.updateAlertRule(alertRuleVo);
        }
        return alertRuleVo.getId();
    }
}
