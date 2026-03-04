/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
    public Object myDoService(JSONObject jsonObj) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
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
