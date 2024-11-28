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
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.dto.ElasticsearchVo;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexBase;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@Component
public class ElasticsearchAlertIndex extends ElasticsearchIndexBase<AlertVo> {
    static Logger logger = LoggerFactory.getLogger(ElasticsearchAlertIndex.class);

    @Override
    public String getName() {
        return "ALERT";
    }

    @Override
    protected void myCreateIndex(ElasticsearchVo elasticsearchVo) {
        CreateIndexRequest.Builder esBuilder = new CreateIndexRequest.Builder()
                .index(this.getIndexName())
                .mappings(m -> m
                        .properties("id", p -> p.long_(l -> l))                        // bigint -> long
                        .properties("level", p -> p.integer(i -> i))                  // int -> integer
                        .properties("title", p -> p.text(t -> elasticsearchVo.getConfig().containsKey("analyser") ? t.analyzer(elasticsearchVo.getConfig().getString("analyser")) : t))                     // varchar -> text
                        .properties("alert_time", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss"))) // datetime -> date
                        .properties("type", p -> p.long_(l -> l))                     // bigint -> long
                        .properties("status", p -> p.keyword(k -> k))                 // enum -> keyword
                        .properties("source", p -> p.keyword(k -> k))                 // varchar -> keyword
                        .properties("is_delete", p -> p.boolean_(b -> b))             // tinyint -> boolean
                        .properties("unique_key", p -> p.keyword(k -> k))             // char -> keyword
                        .properties("alert_count", p -> p.integer(i -> i))            // int -> integer
                        .properties("entity_type", p -> p.keyword(k -> k))            // varchar -> keyword
                        .properties("entity_name", p -> p.text(t -> t))               // varchar -> text
                        .properties("ip", p -> p.keyword(k -> k))                     // varchar -> keyword
                        .properties("port", p -> p.keyword(k -> k))                   // varchar -> keyword
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
    protected void myCreateDocument(AlertVo alertVo) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        // 准备文档数据
        Map<String, Object> document = new HashMap<>();
        document.put("id", alertVo.getId());
        document.put("level", alertVo.getLevel());
        document.put("title", alertVo.getTitle());
        document.put("alert_time", sdf.format(alertVo.getAlertTime()));
        document.put("type", alertVo.getType());
        document.put("status", alertVo.getStatus());
        document.put("source", alertVo.getSource());
        document.put("is_delete", false);
        document.put("unique_key", alertVo.getUniqueKey());
        document.put("alert_count", alertVo.getAlertCount());
        document.put("entity_type", alertVo.getEntityType());
        document.put("entity_name", alertVo.getEntityName());
        document.put("ip", alertVo.getIp());
        document.put("port", alertVo.getPort());


        // 创建或更新文档
        IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>()
                .index(getIndexName()) // 索引名称
                .id(alertVo.getId().toString())      // 文档 ID
                .document(document) // 文档内容
                .build();

        // 执行请求
        IndexResponse response = client.index(request);
    }
}
