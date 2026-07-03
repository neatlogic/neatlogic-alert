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
import neatlogic.framework.util.$;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

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


            if (CollectionUtils.isNotEmpty(userIdList)) {
                Set<String> checkUserIdSet = new HashSet<>();
                for (int i = 0; i < userIdList.size(); i++) {
                    String userId = userIdList.getString(i);
                    checkUserIdSet.add(userId);
                    AlertUserVo alertUserVo = new AlertUserVo();
                    alertUserVo.setAlertId(alertVo.getId());
                    alertUserVo.setUserId(userId);
                    alertMapper.insertAlertUser(alertUserVo);
                }

                if (CollectionUtils.isEmpty(alertVo.getUserIdList()) || !checkUserIdSet.equals(new HashSet<>(alertVo.getUserIdList()))) {
                    AlertAuditVo alertAuditVo = new AlertAuditVo();
                    alertAuditVo.setAlertId(alertVo.getId());
                    alertAuditVo.setAttrName("const_userList");
                    alertAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                    alertAuditVo.setInputUser(UserContext.get().getUserUuid(true));
                    if (CollectionUtils.isNotEmpty(alertVo.getUserIdList())) {
                        alertAuditVo.setOldValueList(JSON.parseArray(JSON.toJSONString(alertVo.getUserIdList())));
                    }
                    alertAuditVo.setNewValueList(userIdList);
                    alertAuditMapper.insertAlertAudit(alertAuditVo);
                }
            }
            if (CollectionUtils.isNotEmpty(teamIdList)) {
                Set<String> checkTeamIdSet = new HashSet<>();
                for (int i = 0; i < teamIdList.size(); i++) {
                    String teamId = teamIdList.getString(i);
                    checkTeamIdSet.add(teamId);
                    AlertTeamVo alertTeamVo = new AlertTeamVo();
                    alertTeamVo.setAlertId(alertVo.getId());
                    alertTeamVo.setTeamUuid(teamId);
                    alertMapper.insertAlertTeam(alertTeamVo);
                }
                if (CollectionUtils.isEmpty(alertVo.getTeamIdList()) || !checkTeamIdSet.equals(new HashSet<>(alertVo.getTeamIdList()))) {
                    AlertAuditVo alertAuditVo = new AlertAuditVo();
                    alertAuditVo.setAlertId(alertVo.getId());
                    alertAuditVo.setAttrName("const_teamList");
                    alertAuditVo.setInputFrom(InputFromContext.get().getInputFrom());
                    alertAuditVo.setInputUser(UserContext.get().getUserUuid(true));
                    if (CollectionUtils.isNotEmpty(alertVo.getTeamIdList())) {
                        alertAuditVo.setOldValueList(JSON.parseArray(JSON.toJSONString(alertVo.getTeamIdList())));
                    }
                    alertAuditVo.setNewValueList(teamIdList);
                    alertAuditMapper.insertAlertAudit(alertAuditVo);
                }
            }
            IElasticsearchDocument<AlertVo> indexHandler = ElasticsearchDocumentFactory.getIndex("ALERT");
            indexHandler.updateDocument(alertVo.getId(), new JSONObject() {{
                this.put("userList", userIdList);
                this.put("teamList", teamIdList);
            }}, false);

            alertEventHandlerAuditVo.setResult(new JSONObject() {{
                this.put("userIdList", userIdList);
                this.put("teamIdList", teamIdList);
            }});
        }


        return alertVo;
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
        }};
    }
}
