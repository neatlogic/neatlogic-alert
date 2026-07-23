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
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
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
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.enums.AlertSearchMode;
import neatlogic.framework.dto.ElasticsearchVo;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteDocumentException;
import neatlogic.framework.exception.elasticsearch.ElasticSearchGetDocumentCountException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentBase;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ElasticsearchAlertIndex extends ElasticsearchDocumentBase<AlertVo> {
    static Logger logger = LoggerFactory.getLogger(ElasticsearchAlertIndex.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm||epoch_millis";

    @Resource
    private AlertViewMapper alertViewMapper;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertCommentMapper alertCommentMapper;

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Override
    public String getName() {
        return "ALERT";
    }

    @Override
    public String getLabel() {
        return "告警中心告警信息";
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

    private String transformField(String field, boolean isKeyword) {
        if (isKeyword) {
            return transformField(field) + ".keyword";
        }
        return transformField(field);
    }

    @Override
    public Boolean needPage(AlertVo alertVo) {
        //return alertVo.getFromAlertId() == null;
        return true;
    }

    private JSONArray convertValue(String field, JSONArray value) {
        if (Objects.equals(field, "const_userList")) {
            if (CollectionUtils.isNotEmpty(value)) {
                JSONArray newValue = new JSONArray();
                for (int i = 0; i < value.size(); i++) {
                    newValue.add(value.getString(i).replace("user#", ""));
                }
                return newValue;
            }
        } else if (Objects.equals(field, "const_teamList")) {
            if (CollectionUtils.isNotEmpty(value)) {
                JSONArray newValue = new JSONArray();
                for (int i = 0; i < value.size(); i++) {
                    newValue.add(value.getString(i).replace("team#", ""));
                }
                return newValue;
            }
        }
        return value;
    }

    @Override
    public void mySortQuery(SearchRequest.Builder builder, AlertVo alertVo) {
        if (MapUtils.isEmpty(alertVo.getSortData())) {
            builder.sort(s -> s
                    .field(f -> f
                            .field("updateTime") // 按 updateTime 排序，因为子告警更新后父告警的updateTime也会更新
                            .order(SortOrder.Desc) // 倒序排列
                    )
            );
        } else {
            for (String field : alertVo.getSortData().keySet()) {
                if (field.startsWith("const_")) {
                    String type = alertVo.getSortData().getString(field);
                    builder.sort(s -> s.field(f -> f.field(field.replace("const_", "")).order(
                            type.equalsIgnoreCase("desc") ? SortOrder.Desc : SortOrder.Asc)));
                }
            }
        }
    }

    @Override
    protected void myHighlight(SearchRequest.Builder builder) {

    }

    /**
     * 按“AND 优先于 OR”的规则组合查询条件。
     *
     * @param queryList 查询条件列表
     * @param relList   相邻条件之间的连接符列表
     * @return 组合后的查询，条件为空时返回null
     */
    private Query combineLogicalQuery(List<Query> queryList, JSONArray relList) {
        if (CollectionUtils.isEmpty(queryList)) {
            return null;
        }
        if (queryList.size() == 1) {
            return queryList.get(0);
        }
        // 兼容历史残缺配置：连接符缺失或数量不匹配时，剩余有效条件全部按AND处理。
        if (CollectionUtils.isEmpty(relList) || relList.size() != queryList.size() - 1) {
            return buildAndQuery(queryList);
        }

        List<Query> orQueryList = new ArrayList<>();
        List<Query> andQueryList = new ArrayList<>();
        andQueryList.add(queryList.get(0));
        for (int i = 0; i < relList.size(); i++) {
            String rel = relList.getString(i);
            Query nextQuery = queryList.get(i + 1);
            if ("and".equalsIgnoreCase(rel)) {
                andQueryList.add(nextQuery);
            } else if ("or".equalsIgnoreCase(rel)) {
                orQueryList.add(buildAndQuery(andQueryList));
                andQueryList = new ArrayList<>();
                andQueryList.add(nextQuery);
            } else {
                throw new IllegalArgumentException("Unsupported conditionRel: " + rel);
            }
        }
        orQueryList.add(buildAndQuery(andQueryList));
        if (orQueryList.size() == 1) {
            return orQueryList.get(0);
        }
        return new Query.Builder()
                .bool(builder -> builder.should(orQueryList).minimumShouldMatch("1"))
                .build();
    }

    /**
     * 将一组连续的AND条件组合成查询。
     *
     * @param queryList 查询条件列表
     * @return AND组合查询
     */
    private Query buildAndQuery(List<Query> queryList) {
        if (queryList.size() == 1) {
            return queryList.get(0);
        }
        return new Query.Builder()
                .bool(builder -> builder.must(queryList))
                .build();
    }


    @Override
    public Query myBuildQuery(AlertVo alertVo) {
        JSONObject rule = new JSONObject();

        Query.Builder finalQueryBuilder = new Query.Builder();
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        //id
        if (alertVo.getKeywordId() != null) {
            Query query = Query.of(q -> q.term(r -> r
                    .field("_id")
                    .value(alertVo.getKeywordId()) // 开始时间
            ));
            boolQueryBuilder.must(query);
        }
        //关键字
        if (StringUtils.isNotBlank(alertVo.getKeyword())) {
            boolQueryBuilder.must(new Query.Builder()
                    .multiMatch(m -> m.query(alertVo.getKeyword()).operator(Operator.And).fields("*"))
                    .build());
        }
        //告警时间
        if (alertVo.getUpdateTimeHour() > 0) {
            long now = System.currentTimeMillis();
            /*es7
            Query query = Query.of(q -> q.range(r -> r
                    .field("updateTime")
                    .gte(JsonData.of(now - (long) alertVo.getUpdateTimeHour() * 60 * 60 * 1000))
            ));*/
            /*es8 */
            Query query = Query.of(q -> q.range(r -> r
                    .untyped(u -> u
                            .field("updateTime")
                            .gte(JsonData.of(now - (long) alertVo.getUpdateTimeHour() * 60 * 60 * 1000))
                    )
            ));


            boolQueryBuilder.must(query);
        }
        //关闭状态。AlertVo.isClose 默认值是 0，只在显式查询关闭告警时追加过滤，避免影响现有默认搜索。
        if (alertVo.getIsClose() == 1) {
            boolQueryBuilder.must(Query.of(q -> q.term(t -> t
                    .field("isClose")
                    .value(1)
            )));
        }
        //告警状态
        if (CollectionUtils.isNotEmpty(alertVo.getStatusList())) {
            List<FieldValue> values = alertVo.getStatusList().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            boolQueryBuilder.must(Query.of(q -> q.terms(t -> t
                    .field("status")
                    .terms(v -> v.value(values))
            )));
        }
        //告警级别
        if (CollectionUtils.isNotEmpty(alertVo.getLevelList())) {
            List<FieldValue> values = alertVo.getLevelList().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            boolQueryBuilder.must(Query.of(q -> q.terms(t -> t
                    .field("level")
                    .terms(v -> v.value(values))
            )));
        }
        //告警来源
        if (CollectionUtils.isNotEmpty(alertVo.getSourceList())) {
            List<FieldValue> values = alertVo.getSourceList().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());
            boolQueryBuilder.must(Query.of(q -> q.terms(t -> t
                    .field("source")
                    .terms(v -> v.value(values))
            )));
        }
        //告警标签
        if (CollectionUtils.isNotEmpty(alertVo.getMarkNameList())) {
            for (String markName : alertVo.getMarkNameList()) {
                Query markQuery = Query.of(q -> q.term(t -> t
                        .field("markList")
                        .value(markName)
                ));
                boolQueryBuilder.must(markQuery);
            }
        }
        //处理组
        if (CollectionUtils.isNotEmpty(alertVo.getTeamIdList())) {
            for (String teamId : alertVo.getTeamIdList()) {
                Query markQuery = Query.of(q -> q.term(t -> t
                        .field("teamList")
                        .value(teamId)
                ));
                boolQueryBuilder.must(markQuery);
            }
        }
        //处理人
        if (CollectionUtils.isNotEmpty(alertVo.getUserIdList())) {
            for (String userId : alertVo.getUserIdList()) {
                Query markQuery = Query.of(q -> q.term(t -> t
                        .field("userList")
                        .value(userId)
                ));
                boolQueryBuilder.must(markQuery);
            }
        }

        //置顶自定义属性搜索
        if (CollectionUtils.isNotEmpty(alertVo.getAttrFilterList())) {
            for (AlertAttrFilterVo attrFilterVo : alertVo.getAttrFilterList()) {
                if (CollectionUtils.isNotEmpty(attrFilterVo.getValueList())) {
                    List<FieldValue> values = attrFilterVo.getValueList().stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList());

                    boolQueryBuilder.must(Query.of(q -> q.terms(t -> t
                            .field(transformField(attrFilterVo.getName()))
                            .terms(v -> v.value(values))
                    )));
                }
            }
        }

        if (MapUtils.isEmpty(alertVo.getRule())) {
            if (StringUtils.isNotBlank(alertVo.getViewName())) {
                AlertViewVo alertViewVo = alertViewMapper.getAlertViewByName(alertVo.getViewName());
                rule = alertViewVo.getConfig().getJSONObject("rule");
            }
        } else {
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
                        JSONArray values = convertValue(field, condition.getJSONArray("valueList"));
                        //转换成可以可以搜索的值
                        if (StringUtils.isBlank(field) || StringUtils.isBlank(expression)) {
                            continue; // 跳过无效条件
                        }

                        Query query = null;
                        switch (expression) {
                            case "equal":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(values.stream()
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
                                                    /*es7
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .gt(JsonData.of(values.getString(0)))
                                                    ))*/
                                                    /*es8 */
                                                    Query.of(q -> q.range(r -> r.untyped(u -> u
                                                            .field(transformField(field))
                                                            .gt(JsonData.of(values.getString(0)))
                                                    )))

                                            ))
                                            .build();
                                }
                                break;
                            case "lt":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    /*es7
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .lt(JsonData.of(values.getString(0)))
                                                    ))*/
                                                    /*es8 */
                                                    Query.of(q -> q.range(r -> r.untyped(u -> u
                                                            .field(transformField(field))
                                                            .lt(JsonData.of(values.getString(0)))
                                                    )))

                                            ))
                                            .build();
                                }
                                break;
                            case "gte":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    /*es7
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .gte(JsonData.of(values.getString(0)))
                                                    ))*/
                                                    /*es8*/
                                                    Query.of(q -> q.range(r -> r.untyped(u -> u
                                                            .field(transformField(field))
                                                            .gte(JsonData.of(values.getString(0)))
                                                    )))

                                            ))
                                            .build();
                                }
                                break;
                            case "lte":
                                if (CollectionUtils.isNotEmpty(values)) {
                                    query = new Query.Builder()
                                            .bool(b -> b.must(
                                                    /*es7
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .lte(JsonData.of(values.getString(0)))
                                                    ))*/
                                                    /*es8 */
                                                    Query.of(q -> q.range(r -> r.untyped(u -> u
                                                            .field(transformField(field))
                                                            .lte(JsonData.of(values.getString(0)))
                                                    )))

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
                                                    /*es7
                                                    Query.of(q -> q.range(r -> r
                                                            .field(transformField(field))
                                                            .gte(JsonData.of(values.getString(0))) // 开始时间
                                                            .lte(JsonData.of(values.getString(1))) // 结束时间
                                                    ))*/
                                                    /*es8 */
                                                    Query.of(q -> q.range(r -> r.untyped(u -> u
                                                            .field(transformField(field))
                                                            .gte(JsonData.of(values.getString(0))) // 开始时间
                                                            .lte(JsonData.of(values.getString(1))) // 结束时间
                                                    )))

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

                    Query groupQuery = combineLogicalQuery(queryList, conditionRelList);
                    if (groupQuery != null) {
                        groupQueryList.add(groupQuery);
                    }
                }

                Query ruleQuery = combineLogicalQuery(groupQueryList, conditionGroupRelList);
                if (ruleQuery != null) {
                    // 高级检索规则整体作为根查询必选条件，避免OR分支绕过其他普通筛选项。
                    boolQueryBuilder.must(ruleQuery);
                }
            }
        }
        //最后处理fromAlertId
        if (!Objects.equals(alertVo.getSearchMode(), AlertSearchMode.FLAT.getValue())) {//尽量兼容旧模式，显示声明flat模式才去掉fromAlertId的判断
            if (alertVo.getFromAlertId() == null) {
                boolQueryBuilder.must(new Query.Builder()
                        .bool(b -> b.mustNot(q -> q.exists(e -> e.field("fromAlertId"))))
                        .build());
            } else {
                boolQueryBuilder.must(new Query.Builder()
                        .bool(b -> b.must(q -> q.term(t -> t.field("fromAlertId").value(alertVo.getFromAlertId()))))
                        .build());
            }
        }

        finalQueryBuilder.bool(boolQueryBuilder.build());
        return finalQueryBuilder.build();
    }

    protected boolean isDocumentExists(AlertVo alertVo) {
        try {
            ElasticsearchClient client = ElasticsearchClientFactory.getClient();
            ExistsRequest existsRequest = new ExistsRequest.Builder()
                    .index(this.getCurrentIndexName())
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
        AlertVo alertVo = new AlertVo();
        alertVo.setPageSize(100);
        alertVo.setCurrentPage(1);
        alertVo.setSearchMode(AlertSearchMode.FLAT.getValue());
        List<AlertVo> alertList = alertMapper.searchAlert(alertVo);
        while (CollectionUtils.isNotEmpty(alertList)) {
            for (AlertVo alert : alertList) {
                if (isAll || !this.isDocumentExists(alert)) {
                    alert.setCommentList(alertCommentMapper.getAlertCommentByAlertId(alert.getId()));
                    try {
                        this.createDocument(alert);
                    } catch (Exception ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                }
            }
            alertVo.setCurrentPage(alertVo.getCurrentPage() + 1);
            alertList = alertMapper.searchAlert(alertVo);
        }
    }


    @Override
    protected AlertVo myGetDocument(AlertVo alertVo) {
        try {
            ElasticsearchClient client = ElasticsearchClientFactory.getClient();
            GetRequest existsRequest = new GetRequest.Builder()
                    .index(this.getCurrentIndexName())
                    .id(alertVo.getId().toString())
                    .build();
            GetResponse<JSONObject> response = client.get(existsRequest, JSONObject.class);
            JSONObject returnObj = response.source();
            // Fastjson 2 的 JSONObject 转 Bean 不会触发 teamList 自定义反序列化器，需从 JSON 文本解析。
            return JSON.parseObject(returnObj.toJSONString(), AlertVo.class);// t
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    protected void myCreateIndex(ElasticsearchVo elasticsearchVo) {
        CreateIndexRequest.Builder esBuilder = new CreateIndexRequest.Builder()
                .index(this.getCurrentIndexName())
                .settings(s -> s
                        .analysis(a -> a
                                .normalizer("lowercase_normalizer", n -> n
                                        .custom(c -> c
                                                .filter("lowercase")
                                        )
                                )
                        ))
                .mappings(buildCreateIndexMapping(elasticsearchVo));
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

    private TypeMapping buildCreateIndexMapping(ElasticsearchVo elasticsearchVo) {
        return TypeMapping.of(m -> m
                .properties("id", p -> p.long_(l -> l))                        // bigint -> long
                .properties("fromAlertId", p -> p.long_(l -> l))
                .properties("level", p -> p.integer(i -> i))                  // int -> integer
                .properties("title", p -> p.text(t -> elasticsearchVo.getConfig().containsKey("analyser") ? t.analyzer(elasticsearchVo.getConfig().getString("analyser")) : t))
                .properties("updateTime", p -> p.date(d -> d.format(DATE_FORMAT)))// varchar -> text
                .properties("alertTime", p -> p.date(d -> d.format(DATE_FORMAT))) // datetime -> date
                .properties("closeTime", p -> p.date(d -> d.format(DATE_FORMAT))) // datetime -> date
                .properties("isClose", p -> p.integer(i -> i))
                .properties("type", p -> p.long_(l -> l))                     // bigint -> long
                .properties("status", p -> p.keyword(k -> k))                 // enum -> keyword
                .properties("source", p -> p.keyword(k -> k.normalizer("lowercase_normalizer")))                 // varchar -> keyword
                .properties("uniqueKey", p -> p.keyword(k -> k))             // char -> keyword
                .properties("userList", p -> p.keyword(k -> k))              // 字符串数组，不分词
                .properties("teamList", p -> p.keyword(k -> k))              // 字符串数组，不分词
                .properties("markList", p -> p.keyword(k -> k)) // 字符串数组，不分词
                //.properties("attrObj", p -> p.object(o -> o.dynamic(DynamicMapping.True)))
                .properties("attrObj", p -> p.flattened(f -> f))
        );
    }

    @Override
    protected Map<String, Property> getCreateIndexMappingPropertyMap(ElasticsearchVo elasticsearchVo) {
        return buildCreateIndexMapping(elasticsearchVo).properties();
    }

    @Override
    protected void myDeleteDocument(Long targetId) {
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .index(this.getCurrentIndexName())
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
    public Map<String, Object> makeupDocument(AlertVo alertVo) {
        // 准备文档数据
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();

        Map<String, Object> document = new HashMap<>();
        document.put("id", alertVo.getId());
        document.put("fromAlertId", alertVo.getFromAlertId());
        document.put("level", alertVo.getLevel());
        document.put("title", alertVo.getTitle());
        document.put("updateTime", alertVo.getUpdateTime() != null ? sdf.format(alertVo.getUpdateTime()) : null);
        document.put("alertTime", alertVo.getAlertTime() != null ? sdf.format(alertVo.getAlertTime()) : null);
        document.put("closeTime", alertVo.getCloseTime() != null ? sdf.format(alertVo.getCloseTime()) : null);
        document.put("type", alertVo.getType());
        document.put("isClose", alertVo.getIsClose());
        document.put("status", alertVo.getStatus());
        document.put("source", alertVo.getSource());
        document.put("uniqueKey", alertVo.getUniqueKey());
        document.put("attrObj", alertVo.getAttrObj(attrTypeList));
        document.put("commentList", getCommentList(alertVo));
        document.put("userList", alertVo.getUserIdList());
        document.put("teamList", alertVo.getTeamIdList());
        document.put("markList", alertVo.getMarkNameList());
        return document;
    }

    /**
     * commentList 保持对象数组结构，只复制 AlertCommentVo 自身字段，避免继承的分页字段被动态 mapping 收进索引。
     */
    private List<Map<String, Object>> getCommentList(AlertVo alertVo) {
        List<Map<String, Object>> commentList = new ArrayList<>();
        if (alertVo == null || CollectionUtils.isEmpty(alertVo.getCommentList())) {
            return commentList;
        }
        for (AlertCommentVo commentVo : alertVo.getCommentList()) {
            if (commentVo != null) {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("id", commentVo.getId());
                commentMap.put("alertId", commentVo.getAlertId());
                commentMap.put("comment", commentVo.getComment());
                commentMap.put("commentUser", commentVo.getCommentUser());
                commentMap.put("commentTime", commentVo.getCommentTime());
                commentList.add(commentMap);
            }
        }
        return commentList;
    }

    @Override
    protected void myCreateDocument(AlertVo alertVo) {
        Map<String, Object> document = makeupDocument(alertVo);
        this.createDocument(alertVo.getId(), document);
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
