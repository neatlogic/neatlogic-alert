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

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.asynchronization.threadlocal.InputFromContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.framework.util.$;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class AlertApplyEventHandler extends AlertEventHandlerBase {


    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertAuditMapper alertAuditMapper;


    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (MapUtils.isNotEmpty(config)) {
            JSONArray userIdList = config.getJSONArray("userIdList");
            JSONArray teamIdList = config.getJSONArray("teamIdList");
            List<String> oldUserIdList = copyIdList(alertVo.getUserIdList());
            List<String> oldTeamIdList = copyIdList(alertVo.getTeamIdList());
            List<String> finalUserIdList = mergeAssignmentIdList(oldUserIdList, userIdList);
            List<String> finalTeamIdList = mergeAssignmentIdList(oldTeamIdList, teamIdList);
            boolean hasAssignment = CollectionUtils.isNotEmpty(userIdList) || CollectionUtils.isNotEmpty(teamIdList);


            if (CollectionUtils.isNotEmpty(userIdList)) {
                for (int i = 0; i < userIdList.size(); i++) {
                    String userId = userIdList.getString(i);
                    AlertUserVo alertUserVo = new AlertUserVo();
                    alertUserVo.setAlertId(alertVo.getId());
                    alertUserVo.setUserId(userId);
                    alertMapper.insertAlertUser(alertUserVo);
                }

                if (!new HashSet<>(oldUserIdList).equals(new HashSet<>(finalUserIdList))) {
                    AlertAuditVo alertAuditVo = new AlertAuditVo();
                    alertAuditVo.setAlertId(alertVo.getId());
                    alertAuditVo.setAttrName("const_userList");
                    alertAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                    alertAuditVo.setInputUser(UserContext.get().getUserUuid(true));
                    if (CollectionUtils.isNotEmpty(oldUserIdList)) {
                        alertAuditVo.setOldValueList(JSON.parseArray(JSON.toJSONString(oldUserIdList)));
                    }
                    alertAuditVo.setNewValueList(JSON.parseArray(JSON.toJSONString(finalUserIdList)));
                    alertAuditMapper.insertAlertAudit(alertAuditVo);
                }
            }
            if (CollectionUtils.isNotEmpty(teamIdList)) {
                for (int i = 0; i < teamIdList.size(); i++) {
                    String teamId = teamIdList.getString(i);
                    AlertTeamVo alertTeamVo = new AlertTeamVo();
                    alertTeamVo.setAlertId(alertVo.getId());
                    alertTeamVo.setTeamUuid(teamId);
                    alertMapper.insertAlertTeam(alertTeamVo);
                }
                if (!new HashSet<>(oldTeamIdList).equals(new HashSet<>(finalTeamIdList))) {
                    AlertAuditVo alertAuditVo = new AlertAuditVo();
                    alertAuditVo.setAlertId(alertVo.getId());
                    alertAuditVo.setAttrName("const_teamList");
                    alertAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                    alertAuditVo.setInputUser(UserContext.get().getUserUuid(true));
                    if (CollectionUtils.isNotEmpty(oldTeamIdList)) {
                        alertAuditVo.setOldValueList(JSON.parseArray(JSON.toJSONString(oldTeamIdList)));
                    }
                    alertAuditVo.setNewValueList(JSON.parseArray(JSON.toJSONString(finalTeamIdList)));
                    alertAuditMapper.insertAlertAudit(alertAuditVo);
                }
            }
            if (hasAssignment) {
                // 事务提交后按数据库最新关系重建完整文档，避免部分字段覆盖造成数据库与ES不一致。
                AfterTransactionJob<Long> indexJob = new AfterTransactionJob<>("ALERT-APPLY-INDEX");
                indexJob.execute(alertVo.getId(), alertId -> {
                    IElasticsearchDocument<AlertVo> indexHandler = ElasticsearchDocumentFactory.getIndex("ALERT");
                    indexHandler.createDocument(alertId);
                });

                alertVo.setUserList(alertMapper.getAlertUserByAlertId(alertVo.getId()));
                alertVo.setTeamList(alertMapper.getAlertTeamByAlertId(alertVo.getId()));
                JSONObject result = new JSONObject();
                result.put("userIdList", alertVo.getUserIdList());
                result.put("teamIdList", alertVo.getTeamIdList());
                alertEventHandlerAuditVo.setResult(result);
            }
        }


        return alertVo;
    }

    /**
     * 复制旧处理对象列表，避免合并过程修改事件上下文中的原始集合。
     */
    private List<String> copyIdList(List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(idList);
    }

    /**
     * APPLY采用追加语义，保留已有处理对象并按配置顺序追加尚未存在的对象。
     */
    static List<String> mergeAssignmentIdList(List<String> oldIdList, JSONArray applyIdList) {
        Set<String> finalIdSet = new LinkedHashSet<>();
        if (CollectionUtils.isNotEmpty(oldIdList)) {
            finalIdSet.addAll(oldIdList);
        }
        if (CollectionUtils.isNotEmpty(applyIdList)) {
            for (int i = 0; i < applyIdList.size(); i++) {
                String id = applyIdList.getString(i);
                if (id != null) {
                    finalIdSet.add(id);
                }
            }
        }
        return new ArrayList<>(finalIdSet);
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public int getSort() {
        return 2;
    }

    @Override
    public String getName() {
        return "APPLY";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.applyhandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-team-s";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.applyhandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
            this.add("ai_agent");
        }};
    }
}
