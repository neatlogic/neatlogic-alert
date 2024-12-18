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

package neatlogic.module.alert.api.alertevent;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertEventVo;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertEventApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "alert/event/list";
    }

    @Override
    public String getName() {
        return "列出所有告警事件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = AlertEventVo[].class)})
    @Description(desc = "列出所有告警事件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AlertEventVo> eventList = new ArrayList<>();
        for (AlertEventType event : AlertEventType.values()) {
            AlertEventVo alertEventVo = new AlertEventVo();
            alertEventVo.setName(event.getName());
            alertEventVo.setLabel(event.getLabel());
            alertEventVo.setDescription(event.getDescription());
            eventList.add(alertEventVo);
        }
        return eventList;
    }


}
