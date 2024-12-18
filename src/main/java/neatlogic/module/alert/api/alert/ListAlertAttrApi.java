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

package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertAttrApi extends PrivateApiComponentBase {

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Override
    public String getToken() {
        return "/alert/alert/attr/list";
    }

    @Override
    public String getName() {
        return "返回告警属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "title", desc = "标题", type = ApiParamType.STRING),
            @Param(name = "type", desc = "告警类型", type = ApiParamType.LONG),
            @Param(name = "status", desc = "状态", type = ApiParamType.STRING),
            @Param(name = "level", desc = "级别", type = ApiParamType.INTEGER),
            @Param(name = "entityType", desc = "对象类型", type = ApiParamType.STRING),
            @Param(name = "entityName", desc = "对象名称", type = ApiParamType.STRING)
    })
    @Description(desc = "返回告警属性列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        List<AlertAttrDefineVo> attrList = AlertAttr.getConstAttrList();
        List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
        for (AlertAttrTypeVo attrTypeVo : attrTypeList) {
            attrList.add(new AlertAttrDefineVo("attr_" + attrTypeVo.getName(), attrTypeVo.getLabel(), "attr", attrTypeVo.getType()));
        }
        return attrList;
    }

}
