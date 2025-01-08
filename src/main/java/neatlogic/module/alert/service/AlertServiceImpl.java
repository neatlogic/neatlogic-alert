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

package neatlogic.module.alert.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.InlineScript;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alert.AlertHasNotAuthException;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.dto.elasticsearch.IndexResultVo;
import neatlogic.framework.exception.elasticsearch.ElasticSearchDeleteFieldException;
import neatlogic.framework.exception.elasticsearch.ElasticSearchIndexNotFoundException;
import neatlogic.framework.store.elasticsearch.ElasticsearchClientFactory;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.module.alert.aftertransaction.ChildAlertStatusUpdateJob;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertCommentMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements IAlertService {
    private final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);
    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertCommentMapper alertCommentMapper;

    @Resource
    private AlertAuditMapper alertAuditMapper;
    @Resource
    private AlertEventMapper alertEventMapper;

    @Override
    public void deleteAlert(Long alertId, boolean isDeleteChildAlert) throws IOException {
        AlertVo alertVo = alertMapper.getAlertById(alertId);
        if (alertVo != null) {
            IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
            if (isDeleteChildAlert) {
                List<Long> toAlertIdList = alertMapper.listAllToAlertIdByFromAlertId(alertVo.getId());
                if (CollectionUtils.isNotEmpty(toAlertIdList)) {
                    for (Long toAlertId : toAlertIdList) {
                        deleteAlert(toAlertId, false);
                    }
                }
            } else {
                //删除es中fromAlertId，只需要查询直系子节点
                List<Long> toAlertIdList = alertMapper.listToAlertIdByFromAlertId(alertVo.getId());
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
                            if (item.error() != null) {
                                throw new ElasticSearchDeleteFieldException(item.id(), "fromAlertId", item.error().reason());
                            }
                        }
                    }
                }
            }
            index.deleteDocument(alertVo);
            alertMapper.deleteAlertById(alertVo.getId());
            AlertEventManager.doEvent(AlertEventType.ALERT_DELETE, alertVo);
        }
    }

    @Override
    public void handleAlert(AlertVo alertVo) {
        AlertVo oldAlertVo = alertMapper.getAlertById(alertVo.getId());
        if (oldAlertVo == null) {
            throw new AlertNotFoundException(alertVo.getId());
        }
        boolean hasRole = false;
        if (CollectionUtils.isNotEmpty(oldAlertVo.getUserList())) {
            hasRole = oldAlertVo.getUserList().stream().anyMatch(d -> d.getUserId().equals(UserContext.get().getUserUuid(true)));
        }
        if (!hasRole && CollectionUtils.isNotEmpty(oldAlertVo.getTeamIdList())) {
            List<String> userTeamList = UserContext.get().getTeamUuidList();
            hasRole = oldAlertVo.getTeamList().stream().anyMatch(d -> userTeamList.contains(d.getTeamUuid()));
        }
        if (!hasRole) {
            throw new AlertHasNotAuthException();
        }
        boolean hasChange = false;
        if (!oldAlertVo.getStatus().equalsIgnoreCase(alertVo.getStatus())) {
            hasChange = true;
            String oldStatus = oldAlertVo.getStatus();
            oldAlertVo.setStatus(alertVo.getStatus());
            alertMapper.updateAlertStatus(alertVo);

            AlertAuditVo alertAuditVo = new AlertAuditVo();
            alertAuditVo.setAlertId(alertVo.getId());
            alertAuditVo.setAttrName("const_status");
            alertAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
            alertAuditVo.setInputUser(UserContext.get().getUserUuid(true));
            alertAuditVo.addOldValue(oldStatus);
            alertAuditVo.addNewValue(alertVo.getStatus());
            alertAuditMapper.insertAlertAudit(alertAuditVo);

            AlertEventManager.doEvent(AlertEventType.ALERT_STATUE_CHANGE, alertVo);

            if (Objects.equals(1, alertVo.getIsChangeChildAlertStatus())) {
                AfterTransactionJob<AlertVo> afterTransactionJob = new AfterTransactionJob<>("ALERT-STATUS-UPDATER");
                afterTransactionJob.execute(new ChildAlertStatusUpdateJob(alertVo));
            }
        }

        if (CollectionUtils.isNotEmpty(alertVo.getApplyUserList())) {
            List<String> mergedUserIdList = new ArrayList<>();
            if (alertVo.getApplyUserType().equals("replace")) {
                alertMapper.deleteAlertUserByAlertId(alertVo.getId());
            } else {
                mergedUserIdList = alertVo.getUserIdList();
            }
            for (String userId : alertVo.getApplyUserList()) {
                AlertUserVo alertUserVo = new AlertUserVo();
                alertUserVo.setAlertId(alertVo.getId());
                alertUserVo.setUserId(userId);
                alertMapper.insertAlertUser(alertUserVo);
                if (!mergedUserIdList.contains(userId)) {
                    mergedUserIdList.add(userId);
                }
            }
            boolean isEqual = new HashSet<>(alertVo.getUserIdList()).equals(new HashSet<>(mergedUserIdList));
            if (!isEqual) {
                AlertAuditVo alertAuditVo = new AlertAuditVo();
                alertAuditVo.setAlertId(alertVo.getId());
                alertAuditVo.setAttrName("const_userList");
                alertAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                alertAuditVo.setInputUser(UserContext.get().getUserUuid(true));
                alertAuditVo.setOldValueList(JSON.parseArray(JSON.toJSONString(alertVo.getUserIdList())));
                alertAuditVo.setNewValueList(JSON.parseArray(JSON.toJSONString(mergedUserIdList)));
                alertAuditMapper.insertAlertAudit(alertAuditVo);
            }
        }

        if (CollectionUtils.isNotEmpty(alertVo.getApplyTeamList())) {
            List<String> mergedTeamIdList = new ArrayList<>();
            if (alertVo.getApplyTeamType().equals("replace")) {
                alertMapper.deleteAlertTeamByAlertId(alertVo.getId());
            } else {
                mergedTeamIdList = alertVo.getTeamIdList();
            }
            for (String teamUuid : alertVo.getApplyTeamList()) {
                AlertTeamVo alertTeamVo = new AlertTeamVo();
                alertTeamVo.setAlertId(alertVo.getId());
                alertTeamVo.setTeamUuid(teamUuid);
                alertMapper.insertAlertTeam(alertTeamVo);
                if (!mergedTeamIdList.contains(teamUuid)) {
                    mergedTeamIdList.add(teamUuid);
                }
            }
            boolean isEqual = new HashSet<>(alertVo.getTeamIdList()).equals(new HashSet<>(mergedTeamIdList));
            if (!isEqual) {
                AlertAuditVo alertAuditVo = new AlertAuditVo();
                alertAuditVo.setAlertId(alertVo.getId());
                alertAuditVo.setAttrName("const_teamList");
                alertAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                alertAuditVo.setInputUser(UserContext.get().getUserUuid(true));
                alertAuditVo.setOldValueList(JSON.parseArray(JSON.toJSONString(alertVo.getTeamIdList())));
                alertAuditVo.setNewValueList(JSON.parseArray(JSON.toJSONString(mergedTeamIdList)));
                alertAuditMapper.insertAlertAudit(alertAuditVo);
            }
        }

        if (StringUtils.isNotBlank(alertVo.getComment())) {
            hasChange = true;
            AlertCommentVo alertCommentVo = new AlertCommentVo();
            alertCommentVo.setComment(alertVo.getComment());
            alertCommentVo.setAlertId(alertVo.getId());
            alertCommentVo.setCommentUser(UserContext.get().getUserUuid(true));
            alertCommentMapper.insertAlertComment(alertCommentVo);
        }

        if (hasChange) {
            IElasticsearchIndex<AlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT");
            if (indexHandler == null) {
                throw new ElasticSearchIndexNotFoundException("ALERT");
            }
            indexHandler.createDocument(alertVo.getId());
        }
    }

    @Override
    public void saveAlert(AlertVo alertVo) {
        IElasticsearchIndex<AlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT");
        if (StringUtils.isNotBlank(alertVo.getUniqueKey())) {
            AlertVo oldAlertVo = alertMapper.getAlertByUniqueKey(alertVo.getUniqueKey());
            if (oldAlertVo != null) {
                oldAlertVo.setUpdateTime(alertVo.getUpdateTime());
                if (oldAlertVo.getId().equals(alertVo.getId())) {
                    return;
                }
                AlertRelVo alertRelVo = new AlertRelVo();
                alertRelVo.setFromAlertId(oldAlertVo.getId());
                alertRelVo.setToAlertId(alertVo.getId());
                alertMapper.saveAlertRel(alertRelVo);
                alertVo.setFromAlertId(oldAlertVo.getId());
                alertVo.setFromAlertVo(oldAlertVo);

                alertMapper.updateAlertUpdateTime(oldAlertVo);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                indexHandler.updateDocument(oldAlertVo.getId(), new JSONObject() {{
                    this.put("updateTime", sdf.format(oldAlertVo.getUpdateTime()));
                }});
            }
        }

        alertMapper.insertAlert(alertVo);
        AlertEventManager.doEvent(AlertEventType.ALERT_SAVE, alertVo);

        if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
            alertMapper.saveAlertAttr(alertVo);
        }
        if (indexHandler == null) {
            throw new ElasticSearchIndexNotFoundException("ALERT");
        }
        indexHandler.createDocument(alertVo);
    }

    @Override
    public List<AlertVo> searchAlert(AlertVo alertVo) {
        IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
        IndexResultVo indexResultVo = index.searchDocument(alertVo, alertVo.getCurrentPage(), alertVo.getPageSize());
        if (CollectionUtils.isNotEmpty(indexResultVo.getIdList())) {
            alertVo.setIdList(indexResultVo.getIdList().stream().map(Long::parseLong).collect(Collectors.toList()));
            alertVo.setCurrentPage(indexResultVo.getCurrentPage());
            alertVo.setPageCount(indexResultVo.getPageCount());
            alertVo.setRowNum(indexResultVo.getRowNum());
            return alertMapper.getAlertByIdList(alertVo);
        }
        return new ArrayList<>();
    }

    @Override
    public List<AlertEventHandlerVo> listAlertEventHandler(AlertEventHandlerVo alertEventHandlerVo) {
        // 获取平铺的结果
        List<AlertEventHandlerVo> handlerList = alertEventMapper.listEventHandler(alertEventHandlerVo);

        // 按 ID 映射
        Map<Long, AlertEventHandlerVo> handlerMap = handlerList.stream()
                .collect(Collectors.toMap(AlertEventHandlerVo::getId, h -> h));
        // 构造层级结构
        List<AlertEventHandlerVo> rootHandlers = new ArrayList<>();
        for (AlertEventHandlerVo handler : handlerList) {
            if (handler.getParentId() == null) {
                // 顶层节点
                rootHandlers.add(handler);
            } else {
                // 子节点
                AlertEventHandlerVo parent = handlerMap.get(handler.getParentId());
                if (parent != null) {
                    if (parent.getHandlerList() == null) {
                        parent.setHandlerList(new ArrayList<>());
                    }
                    parent.getHandlerList().add(handler);
                }
            }
        }
        return rootHandlers;
    }
}
