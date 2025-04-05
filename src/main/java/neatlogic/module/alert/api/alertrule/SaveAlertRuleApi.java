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
