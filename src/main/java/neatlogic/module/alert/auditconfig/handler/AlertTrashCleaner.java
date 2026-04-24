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

import neatlogic.framework.alert.dto.AlertTrashVo;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auditconfig.core.AuditCleanerBase;
import neatlogic.framework.healthcheck.dao.mapper.DatabaseFragmentMapper;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import neatlogic.module.alert.dao.mapper.AlertTrashMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class AlertTrashCleaner extends AuditCleanerBase {
    @Resource
    private AlertTrashMapper alertTrashMapper;
    @Resource
    private DatabaseFragmentMapper databaseFragmentMapper;

    @Override
    public String getName() {
        return "ALERT-TRASH";
    }

    @Override
    protected void myClean(int dayBefore) {
        boolean hasDelete = false;
        IElasticsearchDocument<AlertTrashVo> index = ElasticsearchDocumentFactory.getIndex("ALERT_TRASH");
        List<Long> idList = alertTrashMapper.getAlertTrashIdListByDayBefore(dayBefore);
        int batch = 0;
        while (CollectionUtils.isNotEmpty(idList)) {
            for (Long id : idList) {
                index.deleteDocument(id);
            }
            alertTrashMapper.deleteAlertTrashAttrByIdList(idList);
            alertTrashMapper.deleteAlertTrashByIdList(idList);
            hasDelete = true;
            batch += 1;
            if (batch >= 10) {
                break;//避免过度占用数据库时间，每次最多处理10批数据
            }
            idList = alertTrashMapper.getAlertTrashIdListByDayBefore(dayBefore);
        }
        if (hasDelete) {
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "alert_trash");
            databaseFragmentMapper.rebuildTable(TenantContext.get().getDbName(), "alert_trash_attr");
        }
    }
}
