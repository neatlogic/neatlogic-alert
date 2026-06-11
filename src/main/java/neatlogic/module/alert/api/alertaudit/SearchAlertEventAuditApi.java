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

package neatlogic.module.alert.api.alertaudit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerActionAuditVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerAuditVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertEventAuditApi extends PrivateApiComponentBase {

    @Resource
    private AlertAuditMapper alertAuditMapper;
    @Resource
    private AlertBreakerMapper alertBreakerMapper;


    @Override
    public String getToken() {
        return "/alert/event/audit/search";
    }

    @Override
    public String getName() {
        return "搜索告警事件记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "alertId", desc = "告警id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页大小", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = AlertEventHandlerAuditVo[].class)})
    @Description(desc = "搜索告警事件记录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertEventHandlerAuditVo alertEventHandlerAuditVo = JSON.toJavaObject(jsonObj, AlertEventHandlerAuditVo.class);
        List<AlertEventHandlerAuditVo> auditList = alertAuditMapper.searchAlertEventAudit(alertEventHandlerAuditVo);
        makeupBreakerAudit(auditList);
        List<AlertEventHandlerAuditVo> rootAuditList = auditList.stream().filter(d -> d.getParentId() == null).collect(Collectors.toList());
        makeupChildAudit(rootAuditList, auditList);
        return rootAuditList;
    }

    private void makeupBreakerAudit(List<AlertEventHandlerAuditVo> auditList) {
        if (CollectionUtils.isEmpty(auditList)) {
            return;
        }
        List<Long> auditIdList = auditList.stream().map(AlertEventHandlerAuditVo::getId).collect(Collectors.toList());
        List<AlertBreakerAuditVo> breakerAuditList = alertBreakerMapper.getAlertBreakerAuditListByEventHandlerAuditIdList(auditIdList);
        if (CollectionUtils.isEmpty(breakerAuditList)) {
            return;
        }
        makeupBreakerActionAudit(breakerAuditList);
        Map<Long, List<AlertBreakerAuditVo>> breakerAuditMap = breakerAuditList.stream().collect(Collectors.groupingBy(AlertBreakerAuditVo::getEventHandlerAuditId));
        for (AlertEventHandlerAuditVo auditVo : auditList) {
            auditVo.setBreakerAuditList(breakerAuditMap.get(auditVo.getId()));
        }
    }

    private void makeupBreakerActionAudit(List<AlertBreakerAuditVo> breakerAuditList) {
        // 动作审计必须绑定到本次熔断审计，不能按可复用的 stateId 反查，否则会混入历史动作失败。
        List<Long> breakerAuditIdList = breakerAuditList.stream().map(AlertBreakerAuditVo::getId).collect(Collectors.toList());
        List<AlertBreakerActionAuditVo> actionAuditList = alertBreakerMapper.getAlertBreakerActionAuditListByBreakerAuditIdList(breakerAuditIdList);
        if (CollectionUtils.isEmpty(actionAuditList)) {
            return;
        }
        Map<Long, List<AlertBreakerActionAuditVo>> actionAuditMap = actionAuditList.stream().collect(Collectors.groupingBy(AlertBreakerActionAuditVo::getBreakerAuditId));
        for (AlertBreakerAuditVo breakerAuditVo : breakerAuditList) {
            breakerAuditVo.setActionAuditList(actionAuditMap.get(breakerAuditVo.getId()));
        }
    }

    private void makeupChildAudit(List<AlertEventHandlerAuditVo> parentAuditList, List<AlertEventHandlerAuditVo> allAuditList) {
        for (AlertEventHandlerAuditVo auditVo : parentAuditList) {
            List<AlertEventHandlerAuditVo> childAuditList = allAuditList.stream().filter(d -> d.getParentId() != null && d.getParentId().equals(auditVo.getId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(childAuditList)) {
                auditVo.setChildAuditList(childAuditList);
                makeupChildAudit(childAuditList, allAuditList);
            }
        }
    }
}
