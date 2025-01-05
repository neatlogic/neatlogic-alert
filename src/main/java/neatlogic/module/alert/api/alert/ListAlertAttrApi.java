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
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertAttrApi extends PrivateApiComponentBase {

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertViewMapper alertViewMapper;

    @Override
    public String getToken() {
        return "/alert/attr/list";
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
            @Param(name = "viewId", desc = "视图id", type = ApiParamType.LONG)
    })
    @Output({@Param(explode = AlertAttrDefineVo[].class)})
    @Description(desc = "返回告警属性列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        Long viewId = jsonObj.getLong("viewId");
        List<AlertAttrDefineVo> attrList = AlertAttr.getConstAttrList();
        List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
        for (AlertAttrTypeVo attrTypeVo : attrTypeList) {
            attrList.add(new AlertAttrDefineVo("attr_" + attrTypeVo.getName(), attrTypeVo.getLabel(), "attr", attrTypeVo.getType()));
        }
        if (viewId != null) {
            AlertViewVo alertViewVo = alertViewMapper.getAlertViewById(viewId);
            List<AlertAttrDefineVo> finalAttrList = new ArrayList<>();
            if (alertViewVo != null && MapUtils.isNotEmpty(alertViewVo.getConfig()) && alertViewVo.getConfig().containsKey("attrList")) {
                for (int i = 0; i < alertViewVo.getConfig().getJSONArray("attrList").size(); i++) {
                    String attr = alertViewVo.getConfig().getJSONArray("attrList").getString(i);
                    Optional<AlertAttrDefineVo> op = attrList.stream().filter(d -> d.getName().equals(attr)).findAny();
                    op.ifPresent(finalAttrList::add);
                }
            }
            return finalAttrList;
        }
        return attrList;
    }

}
