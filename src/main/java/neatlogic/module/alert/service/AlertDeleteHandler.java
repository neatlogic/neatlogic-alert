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

package neatlogic.module.alert.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import neatlogic.framework.alert.dto.AlertTrashVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.asynchronization.taskmanager.AsyncTaskManager;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteFieldException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service
public class AlertDeleteHandler {
    private final Logger logger = LoggerFactory.getLogger(AlertDeleteHandler.class);
    private AsyncTaskManager<Long> manager;

    @Resource
    private IAlertService alertService;

    @Resource
    private AlertMapper alertMapper;

    @PostConstruct
    public void init() {
        // 只能允许单线程处理，避免交叉删除关系时出现死锁
        manager = AsyncTaskManager.getInstance("ALERT-DELETE-HANDLER", 1, alertId -> {
            try {
                deleteAlertByIdList(alertId);
            } catch (Exception ignored) {
            }
        });
    }

    public void submitDeleteTask(List<Long> alertIdList) {
        manager.submitTask(alertIdList);
    }


    private void deleteAlertByIdList(Long alertId) throws IOException {
        IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
        IElasticsearchIndex<OriginalAlertVo> index_origin = ElasticsearchIndexFactory.getIndex("ALERT_ORIGINAL");
        //修改formAlertId等于当前id的文档
        List<Long> toAlertIdList = alertMapper.listToAlertIdByFromAlertId(alertId);
        if (CollectionUtils.isNotEmpty(toAlertIdList)) {
            ElasticsearchClient client = ElasticsearchClientFactory.getClient();
            BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
            for (Long toAlertId : toAlertIdList) {
                bulkRequestBuilder.operations(op -> op.update(u -> u.index(index.getIndexName()).id(toAlertId.toString()).action(a -> a.script(Script.of(s -> s.inline(InlineScript.of(i -> i.source("ctx._source.remove('fromAlertId')"))))))));
            }
            // 执行批量请求
            BulkRequest bulkRequest = bulkRequestBuilder.build();
            BulkResponse result = client.bulk(bulkRequest);
            if (result.errors()) {
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null && item.error().reason() != null) {
                        logger.warn((new ElasticSearchDeleteFieldException(item.id(), "fromAlertId", item.error().reason())).getMessage());
                    }
                }
            }
        }

        index.deleteDocument(alertId);
        index_origin.deleteDocument(alertId);
        List<Long> fromAlertIdList = alertMapper.listAllFromAlertIdByToAlertId(alertId);
        AlertVo oldAlertVo = alertMapper.getAlertById(alertId);
        if (oldAlertVo != null) {
            alertMapper.deleteAlertById(alertId);
            //保存垃圾数据
            alertService.saveAlertTrash(new AlertTrashVo(oldAlertVo));
            AlertEventManager.doEvent(AlertEventType.ALERT_DELETE, oldAlertVo);
        }
        if (CollectionUtils.isNotEmpty(fromAlertIdList)) {
            for (Long fromAlertId : fromAlertIdList) {
                AlertVo fromAlertVo = alertMapper.getAlertById(fromAlertId);
                if (fromAlertVo != null) {
                    AlertEventManager.doEvent(AlertEventType.ALERT_CONVERGE_OUT, fromAlertVo);
                }
            }
        }
    }
}
