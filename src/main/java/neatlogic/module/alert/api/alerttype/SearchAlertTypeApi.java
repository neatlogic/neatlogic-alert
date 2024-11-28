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

package neatlogic.module.alert.api.alerttype;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertTypeVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertTypeMapper alertTypeMapper;

    @Override
    public String getToken() {
        return "alert/alerttype/search";
    }

    @Override
    public String getName() {
        return "搜索告警类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页大小", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(explode = AlertTypeVo[].class)
    })
    @Description(desc = "搜索告警类型")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertTypeVo alertTypeVo = JSON.toJavaObject(jsonObj, AlertTypeVo.class);
        int rowNum = alertTypeMapper.searchAlertTypeCount(alertTypeVo);
        List<AlertTypeVo> alertTypeList = null;
        if (rowNum > 0) {
            alertTypeVo.setRowNum(rowNum);
            alertTypeList = alertTypeMapper.searchAlertType(alertTypeVo);
        }
        return TableResultUtil.getResult(alertTypeList, alertTypeVo);
    }

}
