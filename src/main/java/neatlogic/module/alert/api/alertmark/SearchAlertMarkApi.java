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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertMarkVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertMarkMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertMarkApi extends PrivateApiComponentBase {


    @Resource
    private AlertMarkMapper alertMarkMapper;

    @Override
    public String getToken() {
        return "/alert/mark/search";
    }

    @Override
    public String getName() {
        return "搜索标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", desc = "common.keyword", type = ApiParamType.STRING),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "common.defaultvalue")
    })
    @Description(desc = "搜索标签")
    @Output({@Param(explode = AlertMarkVo[].class)})
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertMarkVo alertMarkVo = JSON.toJavaObject(jsonObj, AlertMarkVo.class);
        return alertMarkMapper.searchAlertMark(alertMarkVo);
    }
}
