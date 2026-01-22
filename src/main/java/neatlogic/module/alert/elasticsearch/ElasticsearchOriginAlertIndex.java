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

package neatlogic.module.alert.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.dto.ElasticsearchVo;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteDocumentException;
import neatlogic.framework.exception.elasticsearch.ElasticSearchGetDocumentCountException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexBase;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ElasticsearchOriginAlertIndex extends ElasticsearchIndexBase<OriginalAlertVo> {
    static Logger logger = LoggerFactory.getLogger(ElasticsearchOriginAlertIndex.class);


    @Resource
    private AlertMapper alertMapper;

    @Override
    public String getName() {
        return "ALERT_ORIGINAL";
    }

    @Override
    public String getLabel() {
        return "告警中心原始告警信息";
    }


    @Override
    public void mySortQuery(SearchRequest.Builder builder, OriginalAlertVo originalAlertVo) {
        builder.sort(s -> s
                .field(f -> f
                        .field("id") // 按 id 排序
                        .order(SortOrder.Desc) // 倒序排列
                )
        );
    }

    @Override
    protected void myHighlight(SearchRequest.Builder builder) {
        // **添加高亮配置**
        builder.highlight(h -> h
                .fields("content", f -> f.preTags("<strong class=\"highlight\">").postTags("</strong>")) // 设置 content 字段的高亮
                .fields("error", f -> f.preTags("<strong class=\"highlight\">").postTags("</strong>"))  // 如果还要设置其他字段的高亮
        );
    }


    @Override
    public Query myBuildQuery(OriginalAlertVo alertVo) {
        Query.Builder finalQueryBuilder = new Query.Builder();
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        boolean hasCondition = false;
        //id
        if (alertVo.getKeywordId() != null) {
            Query query = Query.of(q -> q.term(r -> r
                    .field("_id")
                    .value(alertVo.getKeywordId()) // 开始时间
            ));
            boolQuery.must(query);
            hasCondition = true;
        }
        //关键字
        if (StringUtils.isNotBlank(alertVo.getKeyword())) {
            boolQuery.must(new Query.Builder()
                    .multiMatch(m -> m.query(alertVo.getKeyword()).operator(Operator.And).fields("*"))
                    .build());
            hasCondition = true;
        }

        if (StringUtils.isNotBlank(alertVo.getError())) {
            boolQuery.must(m -> m.match(mt -> mt.field("error").query(alertVo.getError())));
            hasCondition = true;
        }

        if (StringUtils.isNotBlank(alertVo.getType())) {
            boolQuery.filter(f -> f.term(t -> t.field("type").value(alertVo.getType())));
            hasCondition = true;
        }

        if (StringUtils.isNotBlank(alertVo.getStatus())) {
            boolQuery.filter(f -> f.term(t -> t.field("status").value(alertVo.getStatus())));
            hasCondition = true;
        }

        if (StringUtils.isNotBlank(alertVo.getAdaptor())) {
            boolQuery.filter(f -> f.term(t -> t.field("adaptor").value(alertVo.getAdaptor())));
            hasCondition = true;
        }

        if (StringUtils.isNotBlank(alertVo.getSource())) {
            boolQuery.filter(f -> f.term(t -> t.field("source").value(alertVo.getSource())));
            hasCondition = true;
        }

        if (CollectionUtils.isNotEmpty(alertVo.getTimeRange())) {
            /*es7*/
            boolQuery.filter(f -> f.range(r -> {
                r.field("time");
                r.gte(JsonData.of(alertVo.getTimeRange().get(0)));
                if (alertVo.getTimeRange().size() > 1) {
                    r.lte(JsonData.of(alertVo.getTimeRange().get(1)));
                }
                return r;
            }));
            /*es8
            boolQuery.filter(f -> f.range(r -> r
                    .date(d -> {
                        d.field("time")
                                .gte(alertVo.getTimeRange().get(0));
                        if (alertVo.getTimeRange().size() > 1) {
                            d.lte(alertVo.getTimeRange().get(1));
                        }
                        return d;
                    })
            ));
             */
            hasCondition = true;
        }
        if (hasCondition) {
            finalQueryBuilder.bool(boolQuery.build());
            return finalQueryBuilder.build();
        }
        return null;
    }

    protected boolean isDocumentExists(OriginalAlertVo alertVo) {
        try {
            ElasticsearchClient client = ElasticsearchClientFactory.getClient();
            ExistsRequest existsRequest = new ExistsRequest.Builder()
                    .index(this.getIndexName())
                    .id(alertVo.getId().toString())
                    .build();
            BooleanResponse response = client.exists(existsRequest);
            return response.value(); // t
        } catch (Exception ex) {
            throw new ElasticSearchGetDocumentCountException(ex);
        }
    }

    @Override
    public void myRebuildDocument(boolean isAll) {
        OriginalAlertVo alertVo = new OriginalAlertVo();
        alertVo.setPageSize(100);
        alertVo.setCurrentPage(1);
        List<OriginalAlertVo> alertList = alertMapper.searchAlertOrigin(alertVo);
        while (CollectionUtils.isNotEmpty(alertList)) {
            for (OriginalAlertVo alert : alertList) {
                if (isAll || !this.isDocumentExists(alert)) {
                    this.createDocument(alert);
                }
            }
            alertVo.setCurrentPage(alertVo.getCurrentPage() + 1);
            alertList = alertMapper.searchAlertOrigin(alertVo);
        }
    }


    @Override
    protected OriginalAlertVo myGetDocument(OriginalAlertVo alertVo) {
        try {
            ElasticsearchClient client = ElasticsearchClientFactory.getClient();
            GetRequest existsRequest = new GetRequest.Builder()
                    .index(this.getIndexName())
                    .id(alertVo.getId().toString())
                    .build();
            GetResponse<JSONObject> response = client.get(existsRequest, JSONObject.class);
            JSONObject returnObj = response.source();
            return JSON.toJavaObject(returnObj, OriginalAlertVo.class);// t
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    protected void myCreateIndex(ElasticsearchVo elasticsearchVo) {
        CreateIndexRequest.Builder esBuilder = new CreateIndexRequest.Builder()
                .index(this.getIndexName())
                .settings(s -> s
                        .analysis(a -> a
                                .normalizer("lowercase_normalizer", n -> n
                                        .custom(c -> c
                                                .filter("lowercase")
                                        )
                                )
                        ))
                .mappings(m -> m
                        .properties("id", p -> p.long_(l -> l))
                        .properties("content", p -> p.text(t -> elasticsearchVo.getConfig().containsKey("analyser") ? t.analyzer(elasticsearchVo.getConfig().getString("analyser")) : t))
                        .properties("error", p -> p.text(t -> elasticsearchVo.getConfig().containsKey("analyser") ? t.analyzer(elasticsearchVo.getConfig().getString("analyser")) : t))
                        .properties("time", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm")))
                        .properties("type", p -> p.keyword(k -> k))
                        .properties("adaptor", p -> p.keyword(k -> k))
                        .properties("source", p -> p.keyword(k -> k.normalizer("lowercase_normalizer")))
                        .properties("status", p -> p.keyword(k -> k))
                );
        if (MapUtils.isNotEmpty(elasticsearchVo.getConfig())) {
            if (elasticsearchVo.getConfig().containsKey("numberOfShards")) {
                esBuilder.settings(s -> s.numberOfShards(elasticsearchVo.getConfig().getString("numberOfShards")));
            }
            if (elasticsearchVo.getConfig().containsKey("numberOfReplicas")) {
                esBuilder.settings(s -> s.numberOfReplicas(elasticsearchVo.getConfig().getString("numberOfReplicas")));
            }
        }
        CreateIndexRequest request = esBuilder.build();

        try {
            ElasticsearchClient client = ElasticsearchClientFactory.getClient();
            client.indices().create(request);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected void myDeleteDocument(Long targetId) {
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .index(getIndexName())
                .id(targetId.toString())
                .build();

        try {
            ElasticsearchClient client = ElasticsearchClientFactory.getClient();
            client.delete(deleteRequest);
        } catch (Exception e) {
            throw new ElasticSearchDeleteDocumentException(e);
        }
    }

    @Override
    public Map<String, Object> makeupDocument(OriginalAlertVo alertVo) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 准备文档数据
        Map<String, Object> document = new HashMap<>();
        document.put("id", alertVo.getId());
        document.put("source", alertVo.getSource());
        document.put("content", alertVo.getContent());
        document.put("time", alertVo.getTime() != null ? sdf.format(alertVo.getTime()) : null);
        document.put("type", alertVo.getType());
        document.put("adaptor", alertVo.getAdaptor());
        document.put("status", alertVo.getStatus());
        document.put("error", alertVo.getError());
        return document;
    }

    @Override
    protected void myCreateDocument(OriginalAlertVo alertVo) {
        Map<String, Object> document = makeupDocument(alertVo);
        this.createDocument(alertVo.getId(), document);
    }

    @Override
    protected void myCreateDocument(Long targetId) {
        OriginalAlertVo alertVo = alertMapper.getAlertOriginById(targetId);
        if (alertVo != null) {
            this.myCreateDocument(alertVo);
        }
    }
}
