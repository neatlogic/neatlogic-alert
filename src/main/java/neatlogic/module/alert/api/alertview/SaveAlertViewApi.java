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

package neatlogic.module.alert.api.alertview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertViewAuthVo;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertViewApi extends PrivateApiComponentBase {

    @Resource
    private AlertViewMapper alertViewMapper;


    @Override
    public String getToken() {
        return "alert/view/save";
    }

    @Override
    public String getName() {
        return "保存告警视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "唯一标识", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "label", desc = "名称", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.INTEGER),
            @Param(name = "config", desc = "配置", isRequired = true, type = ApiParamType.JSONOBJECT)
    })
    @Output({@Param(name = "id", desc = "视图id", type = ApiParamType.LONG)})
    @Description(desc = "保存告警视图")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertViewVo alertViewVo = JSON.toJavaObject(jsonObj, AlertViewVo.class);
        Long id = jsonObj.getLong("id");
        if (id != null) {
            alertViewVo.setLcu(UserContext.get().getUserUuid(true));
            alertViewMapper.deleteAlertViewAuthByViewId(id);
        } else {
            alertViewVo.setFcu(UserContext.get().getUserUuid(true));
        }
        alertViewMapper.saveAlertView(alertViewVo);
        if (CollectionUtils.isNotEmpty(alertViewVo.getAlertViewAuthList())) {
            for (AlertViewAuthVo authVo : alertViewVo.getAlertViewAuthList()) {
                authVo.setViewId(alertViewVo.getId());
                alertViewMapper.insertAlertViewAuth(authVo);
            }
        }
        return alertViewVo.getId();
    }


}
