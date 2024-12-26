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

package neatlogic.module.alert.aftertransaction;

import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChildAlertStatusUpdateJob extends NeatLogicThread {
    private static IAlertService alertService;
    private static AlertMapper alertMapper;

    private AlertVo alertVo;

    @Autowired
    public ChildAlertStatusUpdateJob(IAlertService _alertService, AlertMapper _alertMapper) {
        super("ALERT-STATUS-UPDATER");
        this.alertService = _alertService;
        this.alertMapper = _alertMapper;
    }

    public ChildAlertStatusUpdateJob(AlertVo alertVo) {
        super("ALERT-STATUS-UPDATER");
        this.alertVo = alertVo;
    }

    @Override
    protected void execute() {
        List<AlertVo> alertList = alertMapper.getAlertByParentId(alertVo.getId());
        if (CollectionUtils.isNotEmpty(alertList)) {
            for (AlertVo childAlertVo : alertList) {
                childAlertVo.setIsChangeChildAlertStatus(alertVo.getIsChangeChildAlertStatus());
                childAlertVo.setStatus(alertVo.getStatus());
                alertService.handleAlert(childAlertVo);
            }
        }
    }
}
