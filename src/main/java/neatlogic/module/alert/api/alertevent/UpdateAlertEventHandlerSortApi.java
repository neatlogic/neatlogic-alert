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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_EVENT_MODIFY;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_EVENT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class UpdateAlertEventHandlerSortApi extends PrivateApiComponentBase {


    @Resource
    private AlertEventMapper alertEventMapper;

    @Override
    public String getToken() {
        return "alert/event/sort/update";
    }

    @Override
    public String getName() {
        return "更新告警事件排序";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", desc = "id列表，按照顺序重新排序", type = ApiParamType.JSONARRAY)
    })
    @Description(desc = "更新告警事件排序")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        for (int i = 0; i < idList.size(); i++) {
            alertEventMapper.updateAlertEventHandlerSort(idList.getLong(i), i + 1);
        }
        return null;
    }

}
