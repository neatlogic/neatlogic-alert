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
import co.elastic.clients.elasticsearch._types.SortOrder;
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
import neatlogic.framework.alert.dto.AlertTrashVo;
import neatlogic.framework.alert.exception.alert.AlertIndexException;
import neatlogic.framework.dto.ElasticsearchVo;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteDocumentException;
import neatlogic.framework.exception.elasticsearch.ElasticSearchGetDocumentCountException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexBase;
import neatlogic.module.alert.dao.mapper.AlertCommentMapper;
import neatlogic.module.alert.dao.mapper.AlertTrashMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ElasticsearchAlertTrashIndex extends ElasticsearchIndexBase<AlertTrashVo> {
    static Logger logger = LoggerFactory.getLogger(ElasticsearchAlertTrashIndex.class);


    @Resource
    private AlertTrashMapper alertTrashMapper;

    @Resource
    private AlertCommentMapper alertCommentMapper;

    @Override
    public String getName() {
        return "ALERT_TRASH";
    }

    @Override
    public String getLabel() {
        return "告警中心已删除告警信息";
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
    public Boolean needPage(AlertTrashVo alertVo) {
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
    public void mySortQuery(SearchRequest.Builder builder, AlertTrashVo alertTrashVo) {
        builder.sort(s -> s
                .field(f -> f
                        .field("deleteTime") // 按 deleteTime 排序，因为子告警更新后父告警的updateTime也会更新
                        .order(SortOrder.Desc) // 倒序排列
                        .missing("_last")             // 空值放最后
                )
        );
    }

    @Override
    protected void myHighlight(SearchRequest.Builder builder) {

    }


    @Override
    public Query myBuildQuery(AlertTrashVo alertVo) {
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
            Query query = Query.of(q -> q.range(r -> r
                    .field("updateTime")
                    .gte(JsonData.of(now - (long) alertVo.getUpdateTimeHour() * 60 * 60 * 1000)) // 开始时间
            ));
            boolQueryBuilder.must(query);
        }
        //删除时间
        if (alertVo.getDeleteTimeHour() > 0) {
            long now = System.currentTimeMillis();
            Query query = Query.of(q -> q.range(r -> r
                    .field("deleteTime")
                    .gte(JsonData.of(now - (long) alertVo.getDeleteTimeHour() * 60 * 60 * 1000)) // 开始时间
            ));
            boolQueryBuilder.must(query);
        }
        //删除用户
        if (StringUtils.isNotBlank(alertVo.getDeleteUser())) {
            Query query = Query.of(q -> q.term(r -> r
                    .field("deleteUser")
                    .value(alertVo.getDeleteUser()) // 开始时间
            ));
            boolQueryBuilder.must(query);
        }
        //告警状态
        if (StringUtils.isNotBlank(alertVo.getStatus())) {
            Query query = Query.of(q -> q.term(r -> r
                    .field("status")
                    .value(alertVo.getStatus()) // 开始时间
            ));
            boolQueryBuilder.must(query);
        }
        //告警级别
        if (alertVo.getLevel() != null) {
            Query query = Query.of(q -> q.term(r -> r
                    .field("level")
                    .value(alertVo.getLevel()) // 开始时间
            ));
            boolQueryBuilder.must(query);
        }
        //告警来源
        if (StringUtils.isNotBlank(alertVo.getSource())) {
            Query query = Query.of(q -> q.term(r -> r
                    .field("source")
                    .value(alertVo.getSource()) // 开始时间
            ));
            boolQueryBuilder.must(query);
        }


        finalQueryBuilder.bool(boolQueryBuilder.build());
        return finalQueryBuilder.build();
    }

    protected boolean isDocumentExists(AlertTrashVo alertVo) {
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
        AlertTrashVo alertVo = new AlertTrashVo();
        alertVo.setPageSize(100);
        alertVo.setCurrentPage(1);
        List<AlertTrashVo> alertList = alertTrashMapper.searchAlertTrash(alertVo);
        while (CollectionUtils.isNotEmpty(alertList)) {
            for (AlertTrashVo alert : alertList) {
                if (isAll || !this.isDocumentExists(alert)) {
                    alert.setCommentList(alertCommentMapper.getAlertCommentByAlertId(alertVo.getId()));
                    this.createDocument(alert);
                }
            }
            alertVo.setCurrentPage(alertVo.getCurrentPage() + 1);
            alertList = alertTrashMapper.searchAlertTrash(alertVo);
        }
    }


    @Override
    protected AlertTrashVo myGetDocument(AlertTrashVo alertVo) {
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        GetRequest existsRequest = new GetRequest.Builder()
                .index(this.getIndexName())
                .id(alertVo.getId().toString())
                .build();
        try {
            GetResponse<JSONObject> response = client.get(existsRequest, JSONObject.class);
            JSONObject returnObj = response.source();
            return JSON.toJavaObject(returnObj, AlertTrashVo.class);// t
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
                        .properties("id", p -> p.long_(l -> l))                        // bigint -> long
                        .properties("level", p -> p.integer(i -> i))                  // int -> integer
                        .properties("title", p -> p.text(t -> elasticsearchVo.getConfig().containsKey("analyser") ? t.analyzer(elasticsearchVo.getConfig().getString("analyser")) : t))
                        .properties("updateTime", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm||epoch_millis")))// varchar -> text
                        .properties("alertTime", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm||epoch_millis"))) // datetime -> date
                        .properties("deleteTime", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd HH:mm||epoch_millis"))) // datetime -> date
                        .properties("deleteUser", p -> p.keyword(k -> k)) // datetime -> date
                        .properties("isClose", p -> p.integer(i -> i))
                        .properties("type", p -> p.long_(l -> l))                     // bigint -> long
                        .properties("status", p -> p.keyword(k -> k))                 // enum -> keyword
                        .properties("source", p -> p.keyword(k -> k.normalizer("lowercase_normalizer")))                 // varchar -> keyword
                        .properties("uniqueKey", p -> p.keyword(k -> k))             // char -> keyword
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
    protected void myDeleteDocument(Long targetId) {
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .index(getIndexName())
                .id(targetId.toString())
                .build();
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        try {
            client.delete(deleteRequest);
        } catch (Exception e) {
            throw new ElasticSearchDeleteDocumentException(e);
        }
    }

    @Override
    public Map<String, Object> makeupDocument(AlertTrashVo alertVo) {
        // 准备文档数据
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Map<String, Object> document = new HashMap<>();
        document.put("id", alertVo.getId());
        document.put("level", alertVo.getLevel());
        document.put("title", alertVo.getTitle());
        document.put("updateTime", alertVo.getUpdateTime() != null ? sdf.format(alertVo.getUpdateTime()) : null);
        document.put("alertTime", alertVo.getAlertTime() != null ? sdf.format(alertVo.getAlertTime()) : null);
        document.put("deleteTime", alertVo.getDeleteTime() != null ? sdf.format(alertVo.getDeleteTime()) : null);
        document.put("deleteUser", alertVo.getDeleteUser());
        document.put("type", alertVo.getType());
        document.put("isClose", alertVo.getIsClose());
        document.put("status", alertVo.getStatus());
        document.put("source", alertVo.getSource());
        document.put("uniqueKey", alertVo.getUniqueKey());
        document.put("attrObj", alertVo.getAttrObj());
        return document;
    }

    @Override
    protected void myCreateDocument(AlertTrashVo alertVo) {
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        Map<String, Object> document = makeupDocument(alertVo);
        // 创建或更新文档
        IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>()
                .index(getIndexName()) // 索引名称
                .id(alertVo.getId().toString())      // 文档 ID
                .document(document) // 文档内容
                .build();

        // 执行请求
        try {
            client.index(request);
        } catch (Exception ex) {
            //logger.error(ex.getMessage(), ex);
            throw new AlertIndexException(ex);
        }
    }

    @Override
    protected void myCreateDocument(Long targetId) {
        AlertTrashVo alertVo = alertTrashMapper.getAlertTrashById(targetId);
        this.myCreateDocument(alertVo);
    }
}
