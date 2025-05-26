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
import neatlogic.framework.alert.dto.AlertTopoVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertTopoMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertTopoApi extends PrivateApiComponentBase {

    @Resource
    private AlertTopoMapper alertTopoMapper;

    @Override
    public String getToken() {
        return "/alert/topo/search";
    }

    @Override
    public String getName() {
        return "搜索拓扑图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "isActive", type = ApiParamType.INTEGER, desc = "common.isactive"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AlertTopoVo.class)})
    @Description(desc = "搜索拓扑图")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertTopoVo topoVo = JSON.toJavaObject(jsonObj, AlertTopoVo.class);
        List<AlertTopoVo> topoList = null;
        int rowNum = alertTopoMapper.searchAlertTopoCount(topoVo);
        if (rowNum > 0) {
            topoVo.setRowNum(rowNum);
            topoList = alertTopoMapper.searchAlertTopo(topoVo);
        }
        return TableResultUtil.getResult(topoList, topoVo);
    }
}
