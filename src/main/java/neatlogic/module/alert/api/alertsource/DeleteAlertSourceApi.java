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

package neatlogic.module.alert.api.alertsource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_SOURCE_MODIFY;
import neatlogic.framework.alert.dto.AlertSourceVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertSourceMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_SOURCE_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAlertSourceApi extends PrivateApiComponentBase {

    @Resource
    private AlertSourceMapper alertSourceMapper;

    @Override
    public String getToken() {
        return "/alert/source/delete";
    }

    @Override
    public String getName() {
        return "删除告警来源";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "唯一标识")
    })
    @Output({@Param(explode = AlertSourceVo.class)})
    @Description(desc = "删除告警来源")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        String name = jsonObj.getString("name");
        alertSourceMapper.deleteAlertSourceByName(name);
        return null;
    }
}
