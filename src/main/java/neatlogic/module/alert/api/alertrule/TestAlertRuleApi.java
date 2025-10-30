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
