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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertApi extends PrivateApiComponentBase {

    @Resource
    private AlertTypeMapper alertTypeMapper;

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
            @Param(name = "alertType", desc = "告警类型", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "alertContent", desc = "告警内容", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "alertTime", desc = "告警时间，不提供自动生成", type = ApiParamType.LONG),
    })
    @Description(desc = "推送告警")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        JSONObject obj = new JSONObject();
        return null;
    }

}
