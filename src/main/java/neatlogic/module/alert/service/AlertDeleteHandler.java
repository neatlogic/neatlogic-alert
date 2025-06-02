/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
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

package neatlogic.module.alert.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.asynchronization.queue.NeatLogicNonBlockingQueue;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteFieldException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AlertDeleteHandler {
    private final Logger logger = LoggerFactory.getLogger(AlertDeleteHandler.class);

    //删除告警队列
    private final NeatLogicNonBlockingQueue<Long> deleteAlertQueue = new NeatLogicNonBlockingQueue<>();
    //标记删除线程是否已经启动
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    // 当前运行的 worker 数量
    private final AtomicInteger activeWorkerCount = new AtomicInteger(0);
    // 只能允许单线程处理，避免交叉删除关系时出现死锁
    private static final int MAX_CONCURRENT_WORKERS = 1;
    @Resource
    private AlertMapper alertMapper;

    public void submitDeleteTask(List<Long> alertIdList) {
        alertIdList.forEach(deleteAlertQueue::offer);
        if (isRunning.compareAndSet(false, true)) {
            startWorkers();
        }
    }

    // 启动 worker 线程
    private void startWorkers() {
        for (int i = 0; i < MAX_CONCURRENT_WORKERS; i++) {
            CachedThreadPool.execute(new NeatLogicThread("ALERT-DELETE-WORKER-" + i, false) {
                @Override
                protected void execute() {
                    consumeQueue();
                }
            });
            activeWorkerCount.incrementAndGet();
        }
    }

    // 消费队列的逻辑
    private void consumeQueue() {
        try {
            while (true) {
                Long alertId = deleteAlertQueue.poll();
                if (alertId == null) {
                    break;
                }
                try {
                    deleteAlertByIdList(alertId);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } finally {
            if (activeWorkerCount.decrementAndGet() == 0) {
                // 所有线程退出后检查是否还有任务
                isRunning.set(false);
                if (!deleteAlertQueue.isEmpty() && isRunning.compareAndSet(false, true)) {
                    startWorkers();
                }
            }
        }
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
                bulkRequestBuilder.operations(op -> op.update(u -> u
                        .index(index.getIndexName())
                        .id(toAlertId.toString())
                        .action(a -> a.script(Script.of(s -> s.inline(InlineScript.of(i -> i.source("ctx._source.remove('fromAlertId')"))))))
                ));
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
