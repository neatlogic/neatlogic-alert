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
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import neatlogic.framework.alert.crossover.IAlertEmbeddingCrossoverService;
import neatlogic.framework.alert.crossover.IAlertSuppressionCrossoverService;
import neatlogic.framework.alert.dto.AlertTrashVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.asynchronization.taskmanager.AsyncTaskManager;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadpool.ScheduledThreadPool;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteFieldException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertTrashMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertDeleteHandler {
    private static final int RECOVERY_PAGE_SIZE = 1000;
    private static final long MAX_RETRY_DELAY_SECONDS = 300L;
    private final Logger logger = LoggerFactory.getLogger(AlertDeleteHandler.class);
    private final Set<String> pendingTaskKeySet = ConcurrentHashMap.newKeySet();
    private AsyncTaskManager<AlertDeleteTask> manager;

    @Resource
    private IAlertService alertService;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertTrashMapper alertTrashMapper;

    @PostConstruct
    public void init() {
        // 只能允许单线程处理，避免交叉删除关系时出现死锁。
        manager = AsyncTaskManager.getInstance("ALERT-DELETE-HANDLER", 1, this::executeDeleteTask);
    }

    /**
     * 提交告警删除任务，并按租户和告警编号过滤当前进程中的重复任务。
     *
     * @param alertIdList 告警编号列表
     */
    public void submitDeleteTask(List<Long> alertIdList) {
        if (CollectionUtils.isEmpty(alertIdList)) {
            return;
        }
        String tenantUuid = TenantContext.get().getTenantUuid();
        for (Long alertId : alertIdList) {
            String taskKey = tenantUuid + "#" + alertId;
            if (pendingTaskKeySet.add(taskKey)) {
                manager.submitTask(new AlertDeleteTask(alertId, taskKey, 0));
            }
        }
    }

    /**
     * 分页恢复当前租户中已经标记、但尚未完成物理删除的任务。
     */
    public void recoverPendingDeleteTask() {
        Long currentId = 0L;
        List<Long> alertIdList = alertMapper.getPendingDeleteAlertIdList(currentId, RECOVERY_PAGE_SIZE);
        while (CollectionUtils.isNotEmpty(alertIdList)) {
            submitDeleteTask(alertIdList);
            currentId = alertIdList.get(alertIdList.size() - 1);
            alertIdList = alertMapper.getPendingDeleteAlertIdList(currentId, RECOVERY_PAGE_SIZE);
        }
    }

    /**
     * 执行单个删除任务。失败时保留待删除数据，并通过延迟队列持续重试。
     *
     * @param task 删除任务
     */
    private void executeDeleteTask(AlertDeleteTask task) {
        try {
            deleteAlertById(task.alertId);
            pendingTaskKeySet.remove(task.taskKey);
        } catch (Exception ex) {
            long retryDelay = getRetryDelay(task.retryCount);
            logger.error("删除告警失败，告警编号：{}，将在{}秒后进行第{}次重试", task.alertId, retryDelay, task.retryCount + 1, ex);
            scheduleRetry(task, retryDelay);
        }
    }

    /**
     * 延迟重新提交失败任务，避免外部存储不可用时占满删除线程。
     *
     * @param task       删除任务
     * @param retryDelay 重试延迟秒数
     */
    private void scheduleRetry(AlertDeleteTask task, long retryDelay) {
        AlertDeleteTask retryTask = new AlertDeleteTask(task.alertId, task.taskKey, task.retryCount + 1);
        ScheduledThreadPool.execute(new NeatLogicThread("ALERT-DELETE-RETRY") {
            @Override
            protected void execute() {
                manager.submitTask(retryTask);
            }
        }, retryDelay);
    }

    /**
     * 按指数退避计算重试间隔，最长等待五分钟。
     *
     * @param retryCount 已重试次数
     * @return 重试延迟秒数
     */
    private long getRetryDelay(int retryCount) {
        int exponent = retryCount;
        if (exponent > 8) {
            exponent = 8;
        }
        long retryDelay = 1L << exponent;
        if (retryDelay > MAX_RETRY_DELAY_SECONDS) {
            retryDelay = MAX_RETRY_DELAY_SECONDS;
        }
        return retryDelay;
    }

    /**
     * 以告警主表的删除标记作为恢复依据，先完成可重复执行的外部清理，再原子归档并删除数据库数据。
     *
     * @param alertId 告警编号
     */
    private void deleteAlertById(Long alertId) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        AlertVo oldAlertVo = alertMapper.getAlertById(alertId);
        AlertTrashVo alertTrashVo;
        if (oldAlertVo != null) {
            alertTrashVo = new AlertTrashVo(oldAlertVo);
        } else {
            alertTrashVo = alertTrashMapper.getAlertTrashById(alertId);
        }
        if (alertTrashVo == null) {
            logger.warn("告警删除任务对应的主数据和垃圾箱数据均不存在，告警编号：{}", alertId);
            return;
        }
        if (alertTrashVo.getDeleteTime() == null) {
            alertTrashVo.setDeleteTime(new Date());
        }

        IElasticsearchDocument<AlertVo> alertIndex = ElasticsearchDocumentFactory.getIndex("ALERT");
        IElasticsearchDocument<OriginalAlertVo> originalAlertIndex = ElasticsearchDocumentFactory.getIndex("ALERT_ORIGINAL");
        IElasticsearchDocument<AlertTrashVo> alertTrashIndex = ElasticsearchDocumentFactory.getIndex("ALERT_TRASH");
        List<Long> fromAlertIdList = alertMapper.listAllFromAlertIdByToAlertId(alertId);

        // 先建立垃圾箱索引，后续任一步骤失败时仍可从已标记的主表记录重试。
        alertTrashIndex.createDocument(alertTrashVo);
        removeFromAlertIdReference(alertId, alertIndex);
        alertIndex.deleteDocument(alertId);
        originalAlertIndex.deleteDocument(alertId);
        deleteCommercialData(alertId);

        // 数据库归档与主数据删除必须在同一独立事务中提交。
        alertService.archiveAndDeleteAlert(alertTrashVo);
        fireDeleteEvent(oldAlertVo, fromAlertIdList);
    }

    /**
     * 清理其他告警索引中指向当前告警的聚合关系。
     *
     * @param alertId    告警编号
     * @param alertIndex 告警索引处理器
     */
    private void removeFromAlertIdReference(Long alertId, IElasticsearchDocument<AlertVo> alertIndex) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        List<Long> toAlertIdList = alertMapper.listToAlertIdByFromAlertId(alertId);
        if (CollectionUtils.isEmpty(toAlertIdList)) {
            return;
        }
        ElasticsearchClient client = ElasticsearchClientFactory.getClient();
        BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();
        for (Long toAlertId : toAlertIdList) {
            bulkRequestBuilder.operations(operation -> operation.update(update -> update
                    .index(alertIndex.getIndexName(alertIndex.getName()))
                    .id(toAlertId.toString())
                    .action(action -> action.script(Script.of(script -> script
                            .lang("painless")
                            .source("ctx._source.remove('fromAlertId')")
                    )))
            ));
        }
        BulkResponse result = client.bulk(bulkRequestBuilder.build());
        if (result.errors()) {
            for (BulkResponseItem item : result.items()) {
                if (item.error() != null) {
                    if ("document_missing_exception".equals(item.error().type())) {
                        logger.warn("被聚合告警索引不存在，无需清理fromAlertId字段，告警编号：{}", item.id());
                        continue;
                    }
                    String reason = item.error().reason();
                    if (reason == null) {
                        reason = item.error().toString();
                    }
                    throw new ElasticSearchDeleteFieldException(item.id(), "fromAlertId", reason);
                }
            }
        }
    }

    /**
     * 删除商业模块维护的告警派生数据，失败时由主删除任务统一重试。
     *
     * @param alertId 告警编号
     */
    private void deleteCommercialData(Long alertId) {
        IAlertEmbeddingCrossoverService alertEmbeddingService = CrossoverServiceFactory.tryToGetApi(IAlertEmbeddingCrossoverService.class);
        if (alertEmbeddingService != null) {
            alertEmbeddingService.deleteEmbedding(alertId);
        }

        IAlertSuppressionCrossoverService alertSuppressionService = CrossoverServiceFactory.tryToGetApi(IAlertSuppressionCrossoverService.class);
        if (alertSuppressionService != null) {
            alertSuppressionService.deleteSuppressionAuditByAlertId(alertId);
        }
    }

    /**
     * 核心数据提交成功后触发删除和聚合退出事件，事件失败只记录日志，不重复执行已完成的物理删除。
     *
     * @param oldAlertVo      删除前的告警数据
     * @param fromAlertIdList 原聚合告警编号列表
     */
    private void fireDeleteEvent(AlertVo oldAlertVo, List<Long> fromAlertIdList) {
        try {
            if (oldAlertVo != null) {
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
        } catch (Exception ex) {
            Long alertId = null;
            if (oldAlertVo != null) {
                alertId = oldAlertVo.getId();
            }
            logger.error("告警删除数据已提交，但触发删除事件失败，告警编号：{}", alertId, ex);
        }
    }

    private static class AlertDeleteTask {
        private final Long alertId;
        private final String taskKey;
        private final int retryCount;

        private AlertDeleteTask(Long alertId, String taskKey, int retryCount) {
            this.alertId = alertId;
            this.taskKey = taskKey;
            this.retryCount = retryCount;
        }

    }
}
