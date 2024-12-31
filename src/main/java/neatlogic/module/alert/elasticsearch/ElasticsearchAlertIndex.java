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

package neatlogic.module.alert.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.dto.ElasticsearchVo;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteDocumentException;
import neatlogic.framework.exception.elasticsearch.ElasticSearchGetDocumentCountException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexBase;
import neatlogic.module.alert.dao.mapper.AlertCommentMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ElasticsearchAlertIndex extends ElasticsearchIndexBase<AlertVo> {
    static Logger logger = LoggerFactory.getLogger(ElasticsearchAlertIndex.class);

    @Resource
    private AlertViewMapper alertViewMapper;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertCommentMapper alertCommentMapper;

    @Override
    public String getName() {
        return "ALERT";
    }

    @Override
    public String getLabel() {
        return "告警平台告警信息";
    }


    private String transformField(String field) {
        if (field.startsWith("const_")) {
            return field.substring(6); // 去掉 const_ 前缀
        } else if (field.startsWith("attr_")) {
            return "attrObj." + field.substring(5); // 去掉 attr_ 前缀，并匹配 attrObj 中的属性
        } else {
            return field; // 不做修改
        }
    }

    @Override
    public Boolean needPage(AlertVo alertVo) {
        return alertVo.getFromAlertId() == null;
    }


    @Override
    public Query buildQuery(AlertVo alertVo) {
        JSONObject rule = new JSONObject();

        Query.Builder finalQueryBuilder = new Query.Builder();
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        //简单模式需要加上关键字，规则使用视图规则
        if (Objects.equals("simple", alertVo.getMode())) {
            if (StringUtils.isNotBlank(alertVo.getKeyword())) {
                boolQueryBuilder.must(new Query.Builder()
                        .multiMatch(m -> m.query(alertVo.getKeyword()).operator(Operator.And).fields("*"))
                        .build());
            }
            if (StringUtils.isNotBlank(alertVo.getViewName())) {
                AlertViewVo alertViewVo = alertViewMapper.getAlertViewByName(alertVo.getViewName());
                rule = alertViewVo.getConfig().getJSONObject("rule");
            }
        }//高级模式直接使用传入的规则
        else if (Objects.equals("advanced", alertVo.getMode())) {
            rule = alertVo.getRule();
        }

        if (MapUtils.isNotEmpty(rule)) {
            JSONArray conditionGroupList = rule.getJSONArray("conditionGroupList");
            if (CollectionUtils.isNotEmpty(conditionGroupList)) {
                JSONArray conditionGroupRelList = rule.getJSONArray("conditionGroupRelList");
                List<Query> groupQueryList = new ArrayList<>();
                for (int i = 0; i < conditionGroupList.size(); i++) {
                    JSONObject conditionGroup = conditionGroupList.getJSONObject(i);
                    JSONArray conditionList = conditionGroup.getJSONArray("conditionList");
                    JSONArray conditionRelList = conditionGroup.getJSONArray("conditionRelList");
                    List<Query> queryList = new ArrayList<>();
                    for (int j = 0; j < conditionList.size(); j++) {
                        JSONObject condition = conditionList.getJSONObject(j);
                        String expression = condition.getString("expression");
                        String field = condition.getString("id");
                        JSONArray values = condition.getJSONArray("valueList");

                        if (StringUtils.isBlank(field) || StringUtils.isBlank(expression)) {
                            continue; // 跳过无效条件
                        }

                        Query query = null;
                        switch (expression) {
                            case "equal":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.should(values.stream()
                                                    .map(value -> Query.of(q -> q.matchPhrase(ma -> ma.field(transformField(field)).query(value.toString()))))
                                                    .collect(Collectors.toList())))
                                            .build();
                                }
                                break;
                            case "notequal":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.mustNot(values.stream()
                                                    .map(value -> Query.of(q -> q.matchPhrase(ma -> ma.field(transformField(field)).query(value.toString()))))
                                                    .collect(Collectors.toList())))
                                            .build();
                                }
                                break;
                            case "gt":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .gt(JsonData.of(values.getString(0)))
                                                    ))
                                            ))
                                            .build();
                                }
                                break;
                            case "lt":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .lt(JsonData.of(values.getString(0)))
                                                    ))
                                            ))
                                            .build();
                                }
                                break;
                            case "gte":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .gte(JsonData.of(values.getString(0)))
                                                    ))
                                            ))
                                            .build();
                                }
                                break;
                            case "lte":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .lte(JsonData.of(values.getString(0)))
                                                    ))
                                            ))
                                            .build();
                                }
                                break;
                            case "like":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.should(values.stream()
                                                    .map(value -> Query.of(q -> q.match(w -> w.field(transformField(field)).query(value.toString()).operator(Operator.And))))
                                                    .collect(Collectors.toList())))
                                            .build();
                                }
                                break;
                            case "notlike":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.mustNot(values.stream()
                                                    .map(value -> Query.of(q -> q.match(w -> w.field(transformField(field)).query(value.toString()))))
                                                    .collect(Collectors.toList())))
                                            .build();
                                }
                                break;
                            case "range":
                                if (values.size() == 2) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .gte(JsonData.of(values.getString(0))) // 开始时间
                                                            .lte(JsonData.of(values.getString(1))) // 结束时间
                                                    ))
                                            ))
                                            .build();
                                }
                                break;
                            case "is-null":
                                query = new Query.Builder()
                                        .bool(b -> b.mustNot(q -> q.exists(e -> e.field(transformField(field)))))
                                        .build();
                                break;
                            case "is-not-null":
                                query = new Query.Builder()
                                        .exists(e -> e.field(transformField(field)))
                                        .build();
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported expression: " + expression);
                        }
                        if (query != null) {
                            queryList.add(query);
                        }
                    }

                    Query.Builder conditionFinalQueryBuilder = new Query.Builder();
                    BoolQuery.Builder conditionBoolQueryBuilder = new BoolQuery.Builder();
                    if (CollectionUtils.isNotEmpty(conditionRelList)) {
                        for (int j = 0; j < conditionRelList.size(); j++) {
                            String rel = conditionRelList.getString(j);
                            Query currentQuery = queryList.get(j);
                            Query nextQuery = queryList.get(j + 1);
                            // 根据逻辑关系选择 must 或 should
                            if ("and".equalsIgnoreCase(rel)) {
                                conditionBoolQueryBuilder.must(currentQuery);
                                conditionBoolQueryBuilder.must(nextQuery);
                            } else if ("or".equalsIgnoreCase(rel)) {
                                conditionBoolQueryBuilder.should(currentQuery);
                                conditionBoolQueryBuilder.should(nextQuery);
                            } else {
                                throw new IllegalArgumentException("Unsupported conditionRel: " + rel);
                            }
                        }
                    } else {
                        // 如果 conditionRelList 为空，直接将 queryList 的所有查询加入 must
                        for (Query query : queryList) {
                            conditionBoolQueryBuilder.must(query);
                        }
                    }
                    conditionFinalQueryBuilder.bool(conditionBoolQueryBuilder.build());
                    groupQueryList.add(conditionFinalQueryBuilder.build());
                }


                if (CollectionUtils.isNotEmpty(conditionGroupRelList)) {
                    for (int j = 0; j < conditionGroupRelList.size(); j++) {
                        String rel = conditionGroupRelList.getString(j);
                        Query currentQuery = groupQueryList.get(j);
                        Query nextQuery = groupQueryList.get(j + 1);
                        // 根据逻辑关系选择 must 或 should
                        if ("and".equalsIgnoreCase(rel)) {
                            boolQueryBuilder.must(currentQuery);
                            boolQueryBuilder.must(nextQuery);
                        } else if ("or".equalsIgnoreCase(rel)) {
                            boolQueryBuilder.should(currentQuery);
                            boolQueryBuilder.should(nextQuery);
                        } else {
                            throw new IllegalArgumentException("Unsupported conditionRel: " + rel);
                        }
                    }
                } else {
                    // 如果 conditionRelList 为空，直接将 queryList 的所有查询加入 must
                    for (Query query : groupQueryList) {
                        boolQueryBuilder.must(query);
                    }
                }
            }

        }
        //最后处理fromAlertId
        if (alertVo.getFromAlertId() == null) {
            boolQueryBuilder.must(new Query.Builder()
                    .bool(b -> b.mustNot(q -> q.exists(e -> e.field("fromAlertId"))))
                    .build());
        } else {
            boolQueryBuilder.must(new Query.Builder()
                    .bool(b -> b.must(q -> q.term(t -> t.field("fromAlertId").value(alertVo.getFromAlertId()))))
                    .build());
        }

        finalQueryBuilder.bool(boolQueryBuilder.build());
        return finalQueryBuilder.build();
    }

    protected boolean isDocumentExists(AlertVo alertVo) {
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        ExistsRequest existsRequest = new ExistsRequest.Builder()
                .index(this.getIndexName())
                .id(alertVo.getId().toString())
                .build();
        try {
            BooleanResponse response = client.exists(existsRequest);
            return response.value(); // t
        } catch (Exception ex) {
            throw new ElasticSearchGetDocumentCountException(ex);
        }
    }

    @Override
    public void myRebuildDocument(boolean isAll) {
        AlertVo alertVo = new AlertVo();
        alertVo.setPageSize(100);
        alertVo.setCurrentPage(1);
        List<AlertVo> alertList = alertMapper.searchAlert(alertVo);
        while (CollectionUtils.isNotEmpty(alertList)) {
            for (AlertVo alert : alertList) {
                if (isAll || !this.isDocumentExists(alert)) {
                    alert.setCommentList(alertCommentMapper.getAlertCommentByAlertId(alertVo.getId()));
                    this.createDocument(alert);
                }
            }
            alertVo.setCurrentPage(alertVo.getCurrentPage() + 1);
            alertList = alertMapper.searchAlert(alertVo);
        }
    }


    @Override
    protected AlertVo myGetDocument(AlertVo alertVo) {
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        GetRequest existsRequest = new GetRequest.Builder()
                .index(this.getIndexName())
                .id(alertVo.getId().toString())
                .build();
        try {
            GetResponse<JSONObject> response = client.get(existsRequest, JSONObject.class);
            JSONObject returnObj = response.source();
            return JSON.toJavaObject(returnObj, AlertVo.class);// t
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    protected void myCreateIndex(ElasticsearchVo elasticsearchVo) {
        CreateIndexRequest.Builder esBuilder = new CreateIndexRequest.Builder()
                .index(this.getIndexName())
                .mappings(m -> m
                        .properties("id", p -> p.long_(l -> l))                        // bigint -> long
                        .properties("fromAlertId", p -> p.long_(l -> l))
                        .properties("level", p -> p.integer(i -> i))                  // int -> integer
                        .properties("title", p -> p.text(t -> elasticsearchVo.getConfig().containsKey("analyser") ? t.analyzer(elasticsearchVo.getConfig().getString("analyser")) : t))
                        .properties("updateTime", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm")))// varchar -> text
                        .properties("alertTime", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm"))) // datetime -> date
                        .properties("type", p -> p.long_(l -> l))                     // bigint -> long
                        .properties("status", p -> p.keyword(k -> k))                 // enum -> keyword
                        .properties("source", p -> p.keyword(k -> k))                 // varchar -> keyword
                        .properties("uniqueKey", p -> p.keyword(k -> k))             // char -> keyword
                        .properties("entityType", p -> p.keyword(k -> k))            // varchar -> keyword
                        .properties("entityName", p -> p.text(t -> t))               // varchar -> text
                        .properties("ip", p -> p.text(k -> k))                     // varchar -> keyword
                        .properties("port", p -> p.keyword(k -> k))                   // varchar -> keyword
                        .properties("attrObj", p -> p.object(o -> o.dynamic(DynamicMapping.True)))
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
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        try {
            client.indices().create(request);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected void myDeleteDocument(AlertVo alertVo) {
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .index(getIndexName())
                .id(alertVo.getId().toString())
                .build();
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        try {
            client.delete(deleteRequest);
        } catch (Exception e) {
            throw new ElasticSearchDeleteDocumentException(e);
        }
    }

    @Override
    protected void myCreateDocument(AlertVo alertVo) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        // 准备文档数据
        Map<String, Object> document = new HashMap<>();
        document.put("id", alertVo.getId());
        document.put("fromAlertId", alertVo.getFromAlertId());
        document.put("level", alertVo.getLevel());
        document.put("title", alertVo.getTitle());
        document.put("updateTime", sdf.format(alertVo.getUpdateTime()));
        document.put("alertTime", sdf.format(alertVo.getAlertTime()));
        document.put("type", alertVo.getType());
        document.put("status", alertVo.getStatus());
        document.put("source", alertVo.getSource());
        //document.put("isDelete", false);
        document.put("uniqueKey", alertVo.getUniqueKey());
        //document.put("alertCount", alertVo.getAlertCount());
        document.put("entityType", alertVo.getEntityType());
        document.put("entityName", alertVo.getEntityName());
        document.put("ip", alertVo.getIp());
        document.put("port", alertVo.getPort());
        document.put("attrObj", alertVo.getAttrObj());
        document.put("commentList", alertVo.getCommentList());
        //Map<String, Object> document = JSON.parseObject(JSON.toJSONString(alertVo));


        // 创建或更新文档
        IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>()
                .index(getIndexName()) // 索引名称
                .id(alertVo.getId().toString())      // 文档 ID
                .document(document) // 文档内容
                .build();

        // 执行请求
        try {
            client.index(request);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            //throw new ElasticSearchCreateDocumentException(e);
        }
    }

    @Override
    protected void myCreateDocument(Long targetId) {
        AlertVo alertVo = alertMapper.getAlertById(targetId);
        if (alertVo != null) {
            alertVo.setCommentList(alertCommentMapper.getAlertCommentByAlertId(targetId));
            this.myCreateDocument(alertVo);
        }
    }
}
