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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_RULE_MODIFY;
import neatlogic.framework.alert.utils.AlertRuleUtils;
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
@OperationType(type = OperationTypeEnum.OPERATE)
public class TestAlertRuleApi extends PrivateApiComponentBase {

    @Resource
    private AlertRuleMapper alertRuleMapper;

    @Override
    public String getToken() {
        return "/alert/rule/test";
    }

    @Override
    public String getName() {
        return "测试告警特征";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "content", type = ApiParamType.STRING, desc = "测试内容", isRequired = true),
            @Param(name = "ruleList", type = ApiParamType.JSONARRAY, desc = "规则", isRequired = true)})
    @Output({@Param(name = "result", type = ApiParamType.STRING, desc = "测试结果")})
    @Description(desc = "测试告警特征")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        return AlertRuleUtils.doRule(jsonObj.getString("content"), jsonObj.getJSONArray("ruleList"));
    }
}
