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
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "alert/search";
    }

    @Override
    public String getName() {
        return "搜索告警";
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
    @Description(desc = "查询告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
        return index.searchDocument(jsonObj, 1, 20);
    }

}
