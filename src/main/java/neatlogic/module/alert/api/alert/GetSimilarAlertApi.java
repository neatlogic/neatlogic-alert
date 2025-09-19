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
import co.elastic.clients.elasticsearch._types.query_dsl.Like;
import co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetSimilarAlertApi extends PrivateApiComponentBase {


    @Override
    public String getToken() {
        return "/alert/similar/get";
    }

    @Override
    public String getName() {
        return "获取相似告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "title", desc = "告警标题", isRequired = true, type = ApiParamType.STRING),
    })
    @Output({
            @Param(explode = AlertVo.class)
    })
    @Description(desc = "获取告警详情")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        Query query = new Query.Builder()
                .moreLikeThis(new MoreLikeThisQuery.Builder()
                        .fields("title")  // 相似度计算的字段
                        .like(Like.of(d -> d.text(jsonObj.getString("title"))))                       // 输入文本
                        .minTermFreq(1)
                        .minDocFreq(2)
                        .maxQueryTerms(15)
                        .minWordLength(4)
                        .build())
                .build();

        SearchResponse<Map> response = client.search(s -> s
                        .index("tlcb35_alert")
                        .query(query)
                        .size(5),
                Map.class
        );
        List<String> returnList = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            returnList.add(hit.source().toString());
        }
        return returnList;
    }
}
