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
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertRuleVo;
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
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertRuleApi extends PrivateApiComponentBase {

    @Resource
    private AlertRuleMapper alertRuleMapper;

    @Override
    public String getToken() {
        return "/alert/rule/list";
    }

    @Override
    public String getName() {
        return "获取告警规则列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "isActive", type = ApiParamType.INTEGER, desc = "是否激活", rule = "0,1")})
    @Output({@Param(explode = AlertRuleVo[].class)})
    @Description(desc = "获取告警规则列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertRuleVo alertRuleVo = JSON.toJavaObject(jsonObj, AlertRuleVo.class);
        return alertRuleMapper.listAllAlertRule(alertRuleVo);
    }
}
