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

package neatlogic.module.alert.auditconfig.handler;

import neatlogic.framework.alert.crossover.IAlertSuppressionCrossoverService;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auditconfig.core.AuditCleanerBase;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.healthcheck.dao.mapper.DatabaseFragmentMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class AlertOriginCleaner extends AuditCleanerBase {
    @Resource
    private AlertMapper alertMapper;
    @Resource
    private DatabaseFragmentMapper databaseFragmentMapper;

    @Override
    public String getName() {
        return "ALERT-ORIGIN";
    }

    @Override
    protected void myClean(int dayBefore) {
        boolean hasDelete = false;
        List<Long> idList = alertMapper.getNotUsedAlertOriginIdByDayBefore(dayBefore);
        int batch = 0;
        IAlertSuppressionCrossoverService alertSuppressionService = CrossoverServiceFactory.tryToGetApi(IAlertSuppressionCrossoverService.class);
        while (CollectionUtils.isNotEmpty(idList)) {
            alertMapper.deleteAlertOriginByIdList(idList);
            if (alertSuppressionService != null) {
                alertSuppressionService.deleteSuppressionAuditByAlertIdList(idList);
            }
            idList = alertMapper.getNotUsedAlertOriginIdByDayBefore(dayBefore);
            hasDelete = true;
            batch += 1;
            if (batch >= 10) {
                break;//避免过度占用数据库时间，每次最多处理10批数据
            }
        }
        if (hasDelete) {
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "alert_origin");
        }
    }
}
