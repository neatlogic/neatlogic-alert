/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.alert.auditconfig.handler;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auditconfig.core.AuditCleanerBase;
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
        while (CollectionUtils.isNotEmpty(idList)) {
            alertMapper.deleteAlertOriginByIdList(idList);
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
