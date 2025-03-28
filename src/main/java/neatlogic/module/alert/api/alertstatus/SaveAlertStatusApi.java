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

package neatlogic.module.alert.api.alertstatus;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_STATUS_MODIFY;
import neatlogic.framework.alert.dto.AlertStatusVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertStatusMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_STATUS_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertStatusApi extends PrivateApiComponentBase {

    @Resource
    private AlertStatusMapper alertStatusMapper;

    @Override
    public String getToken() {
        return "/alert/status/save";
    }

    @Override
    public String getName() {
        return "保存告警状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "name", desc = "唯一标识", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "label", desc = "名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "color", desc = "颜色", type = ApiParamType.STRING)
    })
    @Description(desc = "保存告警状态")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        Integer count = alertStatusMapper.getAlertStatusCount();
        if (count == null) {
            count = 0;
        } else {
            count += 1;
        }
        AlertStatusVo alertStatusVo = JSON.toJavaObject(jsonObj, AlertStatusVo.class);
        alertStatusVo.setSort(count);
        alertStatusMapper.saveAlertStatus(alertStatusVo);
        return null;
    }

}
