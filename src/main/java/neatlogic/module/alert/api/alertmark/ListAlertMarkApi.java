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

package neatlogic.module.alert.api.alertmark;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertMarkVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertMarkMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertMarkApi extends PrivateApiComponentBase {


    @Resource
    private AlertMarkMapper alertMarkMapper;

    @Override
    public String getToken() {
        return "/alert/mark/list";
    }

    @Override
    public String getName() {
        return "列出符合条件的标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nameList", desc = "名称列表", type = ApiParamType.JSONARRAY, isRequired = true),
    })
    @Description(desc = "列出符合条件的标签")
    @Output({@Param(explode = AlertMarkVo[].class)})
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        JSONArray jsonArray = jsonObj.getJSONArray("nameList");
        List<String> nameList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                nameList.add(jsonArray.getString(i));
            }
        }
        return alertMarkMapper.listAlertMarkByNameList(nameList);
    }
}
