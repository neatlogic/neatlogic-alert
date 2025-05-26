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

package neatlogic.module.alert.api.alerttopo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertTopoWidgetVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertTopoWidgetMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertTopoWidgetApi extends PrivateApiComponentBase {

    @Resource
    private AlertTopoWidgetMapper alertTopoWidgetMapper;

    @Override
    public String getToken() {
        return "/alert/topo/widget/list";
    }

    @Override
    public String getName() {
        return "列出所有拓扑图元";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "type", type = ApiParamType.STRING, desc = "common.type"),
            @Param(name = "shape", type = ApiParamType.STRING, desc = "nmcdd.widgetvo.entityfield.name")})
    @Output({@Param(explode = AlertTopoWidgetVo[].class)})
    @Description(desc = "列出所有拓扑图元")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertTopoWidgetVo widgetVo = JSON.toJavaObject(jsonObj, AlertTopoWidgetVo.class);
        return alertTopoWidgetMapper.listWidget(widgetVo);
    }
}
