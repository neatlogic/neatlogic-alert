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

import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerActionAuditVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerAuditVo;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 负责查询和组装告警事件执行审计树。
 */
@Service
public class AlertEventAuditService {

    @Resource
    private AlertAuditMapper alertAuditMapper;

    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    /**
     * 从任意审计节点向上查找可达的根节点。
     *
     * @param auditId 审计记录ID
     * @return 根审计记录，不存在时返回null
     */
    public AlertEventHandlerAuditVo getRootAudit(Long auditId) {
        AlertEventHandlerAuditVo currentAudit = alertAuditMapper.getAlertEventAuditById(auditId);
        if (currentAudit == null) {
            return null;
        }
        Set<Long> visitedAuditIdSet = new HashSet<>();
        while (currentAudit.getParentId() != null && visitedAuditIdSet.add(currentAudit.getId())) {
            AlertEventHandlerAuditVo parentAudit = alertAuditMapper.getAlertEventAuditById(currentAudit.getParentId());
            if (parentAudit == null) {
                break;
            }
            currentAudit = parentAudit;
        }
        return currentAudit;
    }

    /**
     * 从任意审计节点定位根节点，并按层级分批加载完整子树。
     *
     * @param auditId 审计记录ID
     * @return 完整根审计树，不存在时返回null
     */
    public AlertEventHandlerAuditVo getAuditTree(Long auditId) {
        AlertEventHandlerAuditVo rootAudit = getRootAudit(auditId);
        if (rootAudit == null) {
            return null;
        }
        List<AlertEventHandlerAuditVo> auditList = new ArrayList<>();
        auditList.add(rootAudit);
        Set<Long> visitedAuditIdSet = new HashSet<>();
        visitedAuditIdSet.add(rootAudit.getId());
        List<Long> parentAuditIdList = Collections.singletonList(rootAudit.getId());
        while (CollectionUtils.isNotEmpty(parentAuditIdList)) {
            List<AlertEventHandlerAuditVo> childAuditList = alertAuditMapper.listAlertEventAuditByParentIdList(parentAuditIdList);
            if (CollectionUtils.isEmpty(childAuditList)) {
                break;
            }
            List<Long> nextParentAuditIdList = new ArrayList<>();
            for (AlertEventHandlerAuditVo childAudit : childAuditList) {
                if (visitedAuditIdSet.add(childAudit.getId())) {
                    auditList.add(childAudit);
                    nextParentAuditIdList.add(childAudit.getId());
                }
            }
            parentAuditIdList = nextParentAuditIdList;
        }
        List<AlertEventHandlerAuditVo> rootAuditList = buildAuditTree(auditList);
        if (CollectionUtils.isEmpty(rootAuditList)) {
            return rootAudit;
        }
        return rootAuditList.get(0);
    }

    /**
     * 为平铺审计记录补充熔断信息，并组装父子树。
     *
     * @param auditList 平铺审计记录
     * @return 根审计列表
     */
    public List<AlertEventHandlerAuditVo> buildAuditTree(List<AlertEventHandlerAuditVo> auditList) {
        if (CollectionUtils.isEmpty(auditList)) {
            return new ArrayList<>();
        }
        makeupBreakerAudit(auditList);
        Map<Long, List<AlertEventHandlerAuditVo>> childAuditMap = new HashMap<>();
        List<AlertEventHandlerAuditVo> rootAuditList = new ArrayList<>();
        Set<Long> auditIdSet = auditList.stream().map(AlertEventHandlerAuditVo::getId).collect(Collectors.toSet());
        for (AlertEventHandlerAuditVo auditVo : auditList) {
            if (auditVo.getParentId() == null || !auditIdSet.contains(auditVo.getParentId())) {
                rootAuditList.add(auditVo);
            } else {
                childAuditMap.computeIfAbsent(auditVo.getParentId(), key -> new ArrayList<>()).add(auditVo);
            }
        }
        for (List<AlertEventHandlerAuditVo> childAuditList : childAuditMap.values()) {
            childAuditList.sort(Comparator.comparing(AlertEventHandlerAuditVo::getId));
        }
        for (AlertEventHandlerAuditVo auditVo : auditList) {
            auditVo.setChildAuditList(childAuditMap.get(auditVo.getId()));
        }
        rootAuditList.sort(Comparator.comparing(AlertEventHandlerAuditVo::getId).reversed());
        return rootAuditList;
    }

    /**
     * 批量补充插件审计关联的熔断及熔断动作记录。
     */
    private void makeupBreakerAudit(List<AlertEventHandlerAuditVo> auditList) {
        List<Long> auditIdList = auditList.stream().map(AlertEventHandlerAuditVo::getId).collect(Collectors.toList());
        List<AlertBreakerAuditVo> breakerAuditList = alertBreakerMapper.getAlertBreakerAuditListByEventHandlerAuditIdList(auditIdList);
        if (CollectionUtils.isEmpty(breakerAuditList)) {
            return;
        }
        makeupBreakerActionAudit(breakerAuditList);
        Map<Long, List<AlertBreakerAuditVo>> breakerAuditMap = breakerAuditList.stream()
                .collect(Collectors.groupingBy(AlertBreakerAuditVo::getEventHandlerAuditId));
        for (AlertEventHandlerAuditVo auditVo : auditList) {
            auditVo.setBreakerAuditList(breakerAuditMap.get(auditVo.getId()));
        }
    }

    /**
     * 动作审计按本次熔断审计ID绑定，避免混入相同状态的历史动作。
     */
    private void makeupBreakerActionAudit(List<AlertBreakerAuditVo> breakerAuditList) {
        List<Long> breakerAuditIdList = breakerAuditList.stream().map(AlertBreakerAuditVo::getId).collect(Collectors.toList());
        List<AlertBreakerActionAuditVo> actionAuditList = alertBreakerMapper.getAlertBreakerActionAuditListByBreakerAuditIdList(breakerAuditIdList);
        if (CollectionUtils.isEmpty(actionAuditList)) {
            return;
        }
        Map<Long, List<AlertBreakerActionAuditVo>> actionAuditMap = actionAuditList.stream()
                .collect(Collectors.groupingBy(AlertBreakerActionAuditVo::getBreakerAuditId));
        for (AlertBreakerAuditVo breakerAuditVo : breakerAuditList) {
            breakerAuditVo.setActionAuditList(actionAuditMap.get(breakerAuditVo.getId()));
        }
    }
}
