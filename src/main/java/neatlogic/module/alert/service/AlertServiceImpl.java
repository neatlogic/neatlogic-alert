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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ADMIN;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.enums.AlertAttrType;
import neatlogic.framework.alert.event.AlertEventManager;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alert.AlertHasNotAuthException;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.dto.elasticsearch.IndexResultHighlightVo;
import neatlogic.framework.dto.elasticsearch.IndexResultVo;
import neatlogic.framework.lock.dao.mapper.LockMapper;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.module.alert.dao.mapper.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements IAlertService {

    private final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);
    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertTrashMapper alertTrashMapper;

    @Resource
    private AlertDeleteHandler alertDeleteHandler;

    @Resource
    private LockMapper lockMapper;

    @Resource
    private AlertCommentMapper alertCommentMapper;

    @Resource
    private AlertAuditMapper alertAuditMapper;
    @Resource
    private AlertEventMapper alertEventMapper;

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertMarkMapper alertMarkMapper;

    private boolean updateAlertMark(AlertVo alertVo) {
        alertMarkMapper.deleteAlertAlertMarkByAlertId(alertVo.getId());
        if (CollectionUtils.isNotEmpty(alertVo.getMarkNameList())) {
            for (String markName : alertVo.getMarkNameList()) {
                AlertMarkVo markVo = new AlertMarkVo();
                markVo.setName(markName);
                alertMarkMapper.insertAlertMark(markVo);
                alertMarkMapper.insertAlertAlertMark(alertVo.getId(), markVo.getUuid());
            }
        }
        IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
        index.updateDocument(alertVo.getId(), new JSONObject() {{
            this.put("markList", alertVo.getMarkNameList());
        }}, false);
        return true;
    }

    private boolean updateAlertStatus(AlertVo oldAlertVo, AlertVo alertVo) {
        if (Objects.equals(1, alertVo.getIsChangeChildAlertStatus())) {
            List<AlertVo> childAlertList = alertMapper.getAlertByParentId(alertVo.getId());
            if (CollectionUtils.isNotEmpty(childAlertList)) {
                for (AlertVo childAlertVo : childAlertList) {
                    //把新状态赋予子告警
                    childAlertVo.setStatus(alertVo.getStatus());
                    this.updateAlertStatus(childAlertVo);
                }
            }
        }
        if (!Objects.equals(oldAlertVo.getStatus(), alertVo.getStatus())) {
            alertMapper.updateAlertStatus(alertVo);
            IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
            index.updateDocument(alertVo.getId(), new JSONObject() {{
                this.put("status", alertVo.getStatus());
            }}, false);
            AlertEventManager.doEvent(AlertEventType.ALERT_STATUE_CHANGE, alertVo);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean updateAlertStatus(AlertVo alertVo) {
        AlertVo oldAlertVo = alertMapper.getAlertById(alertVo.getId());
        if (oldAlertVo == null) {
            throw new AlertNotFoundException(alertVo.getId());
        }
        return updateAlertStatus(oldAlertVo, alertVo);
    }

    @Override
    public boolean openAlert(AlertVo alertVo) {
        if (alertVo != null && !Objects.equals(alertVo.getIsClose(), 0)) {
            IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
            if (Objects.equals(1, alertVo.getIsCloseChildAlert())) {
                List<AlertVo> childAlertList = alertMapper.getCloseAlertByParentId(alertVo.getId());
                if (CollectionUtils.isNotEmpty(childAlertList)) {
                    for (AlertVo childAlertVo : childAlertList) {
                        this.openAlert(childAlertVo);
                    }
                }
            }
            index.updateDocument(alertVo.getId(), new JSONObject() {{
                this.put("isClose", 0);
            }}, false);
            alertMapper.updateAlertIsClose(alertVo.getId(), 0);
            AlertAuditVo alertAuditVo = new AlertAuditVo(true);
            alertAuditVo.setAlertId(alertVo.getId());
            alertAuditVo.setAttrName("const_isClose");
            alertAuditVo.addOldValue(1);
            alertAuditVo.addNewValue(0);
            alertAuditMapper.insertAlertAudit(alertAuditVo);
            AlertEventManager.doEvent(AlertEventType.ALERT_OPEN, alertVo);
            return true;
        }
        return false;
    }

    @Override
    public boolean closeAlert(AlertVo alertVo) {
        if (alertVo != null && !Objects.equals(alertVo.getIsClose(), 1)) {
            IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
            if (Objects.equals(1, alertVo.getIsCloseChildAlert())) {
                List<AlertVo> childAlertList = alertMapper.getOpenAlertByParentId(alertVo.getId());
                if (CollectionUtils.isNotEmpty(childAlertList)) {
                    for (AlertVo childAlertVo : childAlertList) {
                        this.closeAlert(childAlertVo);
                    }
                }
            }
            index.updateDocument(alertVo.getId(), new JSONObject() {{
                this.put("isClose", 1);
            }}, false);
            alertMapper.updateAlertIsClose(alertVo.getId(), 1);
            AlertAuditVo alertAuditVo = new AlertAuditVo(true);
            alertAuditVo.setAlertId(alertVo.getId());
            alertAuditVo.setAttrName("const_isClose");
            alertAuditVo.addOldValue(0);
            alertAuditVo.addNewValue(1);
            alertAuditMapper.insertAlertAudit(alertAuditVo);
            AlertEventManager.doEvent(AlertEventType.ALERT_CLOSE, alertVo);
            return true;
        }
        return false;
    }

    /*private void deleteAlertByIdList(List<Long> alertIdList) throws IOException {
        for (Long alertId : alertIdList) {
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
                        if (item.error() != null) {
                            throw new ElasticSearchDeleteFieldException(item.id(), "fromAlertId", item.error().reason());
                        }
                    }
                }
            }

            index.deleteDocument(alertId);
            index_origin.deleteDocument(alertId);
            List<Long> fromAlertIdList = alertMapper.listAllFromAlertIdByToAlertId(alertId);
            AlertVo oldAlertVo = alertMapper.getAlertById(alertId);
            alertMapper.deleteAlertById(alertId);
            AlertEventManager.doEvent(AlertEventType.ALERT_DELETE, oldAlertVo);
            if (CollectionUtils.isNotEmpty(fromAlertIdList)) {
                for (Long fromAlertId : fromAlertIdList) {
                    AlertVo fromAlertVo = alertMapper.getAlertById(fromAlertId);
                    AlertEventManager.doEvent(AlertEventType.ALERT_CONVERGE_OUT, fromAlertVo);
                }
            }
        }
    }*/

    @Override
    public void deleteAlert(List<Long> alertIdList, boolean isDeleteChildAlert) {
        Long deleteBatch = System.currentTimeMillis();
        List<Long> deleteAlertIdList = new ArrayList<>(alertIdList);
        if (isDeleteChildAlert) {
            for (Long alertId : alertIdList) {
                List<Long> toAlertIdList = alertMapper.listAllToAlertIdByFromAlertId(alertId);
                if (CollectionUtils.isNotEmpty(toAlertIdList)) {
                    deleteAlertIdList.addAll(toAlertIdList);
                }
            }
        }
        AlertVo tmpAlertVo = new AlertVo();
        tmpAlertVo.setDeleteBatch(deleteBatch);
        tmpAlertVo.setIdList(deleteAlertIdList);
        alertMapper.updateAlertIsDeleteByIdList(tmpAlertVo);
        AfterTransactionJob<List<Long>> afterTransactionJob = new AfterTransactionJob<>("ALERT-DELETER");
        afterTransactionJob.execute(deleteAlertIdList, idList -> {
            try {
                //deleteAlertByIdList(idList);
                alertDeleteHandler.submitDeleteTask(idList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void deleteAlert(Long alertId, boolean isDeleteChildAlert) {
        Long deleteBatch = System.currentTimeMillis();
        List<Long> deleteAlertIdList = new ArrayList<>();
        deleteAlertIdList.add(alertId);
        if (isDeleteChildAlert) {
            List<Long> toAlertIdList = alertMapper.listAllToAlertIdByFromAlertId(alertId);
            if (CollectionUtils.isNotEmpty(toAlertIdList)) {
                deleteAlertIdList.addAll(toAlertIdList);
            }
        }
        AlertVo tmpAlertVo = new AlertVo();
        tmpAlertVo.setDeleteBatch(deleteBatch);
        tmpAlertVo.setIdList(deleteAlertIdList);
        alertMapper.updateAlertIsDeleteByIdList(tmpAlertVo);
        AfterTransactionJob<List<Long>> afterTransactionJob = new AfterTransactionJob<>("ALERT-DELETER");
        afterTransactionJob.execute(deleteAlertIdList, idList -> {
            try {
                //deleteAlertByIdList(idList);
                alertDeleteHandler.submitDeleteTask(idList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void handleAlert(AlertVo alertVo) {
        AlertVo oldAlertVo = alertMapper.getAlertById(alertVo.getId());
        if (oldAlertVo == null) {
            throw new AlertNotFoundException(alertVo.getId());
        }
        boolean hasRole = AuthActionChecker.check(ALERT_ADMIN.class);
        if (!hasRole && CollectionUtils.isNotEmpty(oldAlertVo.getUserList())) {
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
        if (Objects.equals(1, alertVo.getIsClose())) {
            //需要传入旧告警，否则会导致判断状态错误而不执行
            oldAlertVo.setIsCloseChildAlert(alertVo.getIsCloseChildAlert());
            hasChange = closeAlert(oldAlertVo);
        } else if (Objects.equals(0, alertVo.getIsClose())) {
            //需要传入旧告警，否则会导致判断状态错误而不执行
            oldAlertVo.setIsCloseChildAlert(alertVo.getIsCloseChildAlert());
            hasChange = openAlert(oldAlertVo);
        }


        Set<String> newMarkSet = new HashSet<>();
        Set<String> oldMarkSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(oldAlertVo.getMarkNameList())) {
            oldMarkSet.addAll(oldAlertVo.getMarkNameList());
        }
        if (CollectionUtils.isNotEmpty(alertVo.getMarkNameList())) {
            newMarkSet.addAll(alertVo.getMarkNameList());
        }
        if (!newMarkSet.equals(oldMarkSet)) {
            hasChange = true;

            updateAlertMark(alertVo);

            AlertAuditVo alertAuditVo = new AlertAuditVo(true);
            alertAuditVo.setAlertId(alertVo.getId());
            alertAuditVo.setAttrName("const_markList");
            alertAuditVo.addOldValue(oldAlertVo.getMarkNameList());
            alertAuditVo.addNewValue(alertVo.getMarkNameList());
            alertAuditMapper.insertAlertAudit(alertAuditVo);
        }


        if (!Objects.equals(oldAlertVo.getStatus(), alertVo.getStatus())) {
            hasChange = true;
            updateAlertStatus(oldAlertVo, alertVo);
            String oldStatus = oldAlertVo.getStatus();

            AlertAuditVo alertAuditVo = new AlertAuditVo(true);
            alertAuditVo.setAlertId(alertVo.getId());
            alertAuditVo.setAttrName("const_status");
            alertAuditVo.addOldValue(oldStatus);
            alertAuditVo.addNewValue(alertVo.getStatus());
            alertAuditMapper.insertAlertAudit(alertAuditVo);

            //AlertEventManager.doEvent(AlertEventType.ALERT_STATUE_CHANGE, alertVo);

            /*if (Objects.equals(1, alertVo.getIsChangeChildAlertStatus())) {
                AfterTransactionJob<AlertVo> afterTransactionJob = new AfterTransactionJob<>("ALERT-STATUS-UPDATER");
                afterTransactionJob.execute(new ChildAlertStatusUpdateJob(alertVo));
            }*/
        }

        if (CollectionUtils.isNotEmpty(alertVo.getApplyUserList())) {
            List<String> mergedUserIdList = new ArrayList<>();
            if (alertVo.getApplyUserType().equals("replace")) {
                alertMapper.deleteAlertUserByAlertId(alertVo.getId());
            } else {
                if (CollectionUtils.isNotEmpty(alertVo.getUserIdList())) {
                    mergedUserIdList = alertVo.getUserIdList();
                }
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

            List<String> list1 = alertVo.getUserIdList() != null ? alertVo.getUserIdList() : Collections.emptyList();
            List<String> list2 = mergedUserIdList != null ? mergedUserIdList : Collections.emptyList();
            boolean isEqual = new HashSet<>(list1).equals(new HashSet<>(list2));

            if (!isEqual) {
                hasChange = true;
                AlertAuditVo alertAuditVo = new AlertAuditVo(true);
                alertAuditVo.setAlertId(alertVo.getId());
                alertAuditVo.setAttrName("const_userList");
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
                if (CollectionUtils.isNotEmpty(alertVo.getTeamIdList())) {
                    mergedTeamIdList = alertVo.getTeamIdList();
                }
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
            List<String> list1 = alertVo.getTeamIdList() != null ? alertVo.getTeamIdList() : Collections.emptyList();
            List<String> list2 = mergedTeamIdList != null ? mergedTeamIdList : Collections.emptyList();
            boolean isEqual = new HashSet<>(list1).equals(new HashSet<>(list2));
            if (!isEqual) {
                hasChange = true;
                AlertAuditVo alertAuditVo = new AlertAuditVo(true);
                alertAuditVo.setAlertId(alertVo.getId());
                alertAuditVo.setAttrName("const_teamList");
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
            indexHandler.createDocument(alertVo.getId());
        }
    }

    private AlertVo getAlertById(Long alertId) {
        AlertVo newAlertVo = alertEventMapper.getAlertById(alertId);
        //补充完整的处理人信息和处理组信息
        newAlertVo.setUserList(alertEventMapper.getAlertUserByAlertId(alertId));
        newAlertVo.setTeamList(alertEventMapper.getAlertTeamByAlertId(alertId));
        return newAlertVo;
    }

    @Override
    public void saveOriginAlert(OriginalAlertVo originalAlertVo) {
        IElasticsearchIndex<OriginalAlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT_ORIGINAL");
        indexHandler.createDocument(originalAlertVo);
        alertMapper.insertAlertOrigin(originalAlertVo);
    }

    @Override
    public void saveAlertTrash(AlertTrashVo alertVo) {
        alertVo.setDeleteUser(UserContext.get().getUserUuid(true));
        IElasticsearchIndex<AlertTrashVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT_TRASH");
        alertTrashMapper.insertAlertTrash(alertVo);
        if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
            alertTrashMapper.saveAlertTrashAttr(alertVo);
        }
        indexHandler.createDocument(alertVo);
    }


    @Override
    public void saveAlert(AlertVo alertVo, boolean isSerial) {
        if (isSerial && StringUtils.isNotBlank(alertVo.getUniqueKey())) {
            lockMapper.getMysqlLock(alertVo.getUniqueKey(), 30);
        }
        IElasticsearchIndex<AlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT");
        AlertVo parentAlertVo = null;
        if (StringUtils.isNotBlank(alertVo.getUniqueKey())) {
            Long parentAlertId = alertMapper.getFirstOpenAlertIdByUniqueKey(alertVo.getUniqueKey());
            if (parentAlertId != null) {
                parentAlertVo = getAlertById(parentAlertId);
            }
            if (parentAlertVo != null) {
                alertVo.setParentAlertVo(parentAlertVo);
                //String oldStatus = parentAlertVo.getStatus();
                parentAlertVo.setUpdateTime(alertVo.getUpdateTime());
                //parentAlertVo.setStatus(alertVo.getStatus());
                if (parentAlertVo.getId().equals(alertVo.getId())) {
                    return;
                }
                AlertRelVo alertRelVo = new AlertRelVo();
                alertRelVo.setFromAlertId(parentAlertVo.getId());
                alertRelVo.setToAlertId(alertVo.getId());
                alertMapper.saveAlertRel(alertRelVo);
                alertVo.setFromAlertId(parentAlertVo.getId());
                alertVo.setFromAlertVo(parentAlertVo);

                alertMapper.updateAlertUpdateTime(parentAlertVo);
                Map<String, Object> document = indexHandler.makeupDocument(parentAlertVo);
                indexHandler.updateDocument(parentAlertVo.getId(), document, true);
            }
        }

        alertMapper.insertAlert(alertVo);

        if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
            alertMapper.saveAlertAttr(alertVo);
            //如果是enum型属性则把数据保存起来
            for (Map.Entry<String, Object> entry : alertVo.getAttrObj().entrySet()) {
                AlertAttrTypeVo alertAttrTypeVo = alertAttrTypeMapper.getAttrTypeByName(entry.getKey());
                if (alertAttrTypeVo != null && alertAttrTypeVo.getType().equals(AlertAttrType.ENUM.getValue())) {
                    if (entry.getValue() instanceof JSONArray) {
                        JSONArray valueList = (JSONArray) entry.getValue();
                        for (int i = 0; i < valueList.size(); i++) {
                            if (valueList.get(i) != null) {
                                AlertAttrTypeEnumVo enumVo = new AlertAttrTypeEnumVo();
                                enumVo.setAttrType(alertAttrTypeVo.getId());
                                enumVo.setValue(valueList.getString(i));
                                enumVo.setText(valueList.getString(i));
                                alertAttrTypeMapper.saveAlertAttrTypeEnum(enumVo);
                            }
                        }
                    } else {
                        if (entry.getValue() != null) {
                            AlertAttrTypeEnumVo enumVo = new AlertAttrTypeEnumVo();
                            enumVo.setAttrType(alertAttrTypeVo.getId());
                            enumVo.setValue(entry.getValue().toString());
                            enumVo.setText(entry.getValue().toString());
                            alertAttrTypeMapper.saveAlertAttrTypeEnum(enumVo);
                        }
                    }
                }
            }
        }

        //检查标签是否有值
        if (CollectionUtils.isNotEmpty(alertVo.getMarkNameList())) {
            for (String mark : alertVo.getMarkNameList()) {
                AlertMarkVo alertMarkVo = new AlertMarkVo();
                alertMarkVo.setName(mark);
                alertMarkMapper.insertAlertMark(alertMarkVo);
                alertMarkMapper.insertAlertAlertMark(alertVo.getId(), alertMarkVo.getUuid());
            }
        }

        //检查是否有处理人和处理组，有的话也一并写入
        if (CollectionUtils.isNotEmpty(alertVo.getUserIdList())) {
            for (String userId : alertVo.getUserIdList()) {
                AlertUserVo alertUserVo = new AlertUserVo();
                alertUserVo.setAlertId(alertVo.getId());
                alertUserVo.setUserId(userId);
                alertMapper.insertAlertUser(alertUserVo);
            }
        }

        if (CollectionUtils.isNotEmpty(alertVo.getTeamIdList())) {
            for (String teamId : alertVo.getTeamIdList()) {
                AlertTeamVo alertTeamVo = new AlertTeamVo();
                alertTeamVo.setAlertId(alertVo.getId());
                alertTeamVo.setTeamUuid(teamId);
                alertMapper.insertAlertTeam(alertTeamVo);
            }
        }

        indexHandler.createDocument(alertVo);

        if (alertVo.getParentAlertVo() == null) {
            AlertEventManager.doEvent(AlertEventType.ALERT_SAVE, alertVo);
        } else {
            AlertEventManager.doEvent(AlertEventType.ALERT_CONVERGE, alertVo);
            AlertEventManager.doEvent(AlertEventType.ALERT_CONVERGE_IN, alertVo.getParentAlertVo());
        }


        if (isSerial && StringUtils.isNotBlank(alertVo.getUniqueKey())) {
            lockMapper.releaseMysqlLock(alertVo.getUniqueKey());
        }
    }

    @Override
    public long searchAlertCount(AlertVo alertVo) {
        IElasticsearchIndex<AlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT");
        return index.searchDocumentCount(alertVo);
    }

    @Override
    public List<AlertTrashVo> searchAlertTrash(AlertTrashVo alertTrashVo) {
        IElasticsearchIndex<AlertTrashVo> index = ElasticsearchIndexFactory.getIndex("ALERT_TRASH");
        IndexResultVo indexResultVo = index.searchDocument(alertTrashVo, alertTrashVo.getCurrentPage(), alertTrashVo.getPageSize());
        if (CollectionUtils.isNotEmpty(indexResultVo.getIdList())) {
            alertTrashVo.setIdList(indexResultVo.getIdList().stream().map(Long::parseLong).collect(Collectors.toList()));
            alertTrashVo.setCurrentPage(indexResultVo.getCurrentPage());
            alertTrashVo.setPageCount(indexResultVo.getPageCount());
            alertTrashVo.setRowNum(indexResultVo.getRowNum());
            return alertTrashMapper.getAlertTrashByIdList(alertTrashVo);
        }
        return new ArrayList<>();
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
    public List<OriginalAlertVo> searchOriginAlert(OriginalAlertVo originalAlertVo) {
        IElasticsearchIndex<OriginalAlertVo> index = ElasticsearchIndexFactory.getIndex("ALERT_ORIGINAL");
        IndexResultVo indexResultVo = index.searchDocument(originalAlertVo, originalAlertVo.getCurrentPage(), originalAlertVo.getPageSize());
        if (CollectionUtils.isNotEmpty(indexResultVo.getIdList())) {
            originalAlertVo.setIdList(indexResultVo.getIdList().stream().map(Long::parseLong).collect(Collectors.toList()));
            originalAlertVo.setCurrentPage(indexResultVo.getCurrentPage());
            originalAlertVo.setPageCount(indexResultVo.getPageCount());
            originalAlertVo.setRowNum(indexResultVo.getRowNum());
            List<OriginalAlertVo> alertList = alertMapper.getAlertOriginByIdList(originalAlertVo);
            if (CollectionUtils.isNotEmpty(indexResultVo.getHighlightList())) {
                for (OriginalAlertVo o : alertList) {
                    Optional<IndexResultHighlightVo> op = indexResultVo.getHighlightList().stream().filter(d -> d.getId().equals(o.getId().toString())).findAny();
                    op.ifPresent(indexResultHighlightVo -> o.setHighlightMap(indexResultHighlightVo.getHighlightMap()));
                }
            }
            return alertList;
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
