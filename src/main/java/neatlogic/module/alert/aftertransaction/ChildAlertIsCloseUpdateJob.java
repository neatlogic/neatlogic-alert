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
public class ChildAlertIsCloseUpdateJob extends NeatLogicThread {
    private static IAlertService alertService;
    private static AlertMapper alertMapper;

    private AlertVo alertVo;

    @Autowired
    public ChildAlertIsCloseUpdateJob(IAlertService _alertService, AlertMapper _alertMapper) {
        super("ALERT-CLOSE-UPDATER");
        this.alertService = _alertService;
        this.alertMapper = _alertMapper;
    }

    public ChildAlertIsCloseUpdateJob(AlertVo alertVo) {
        super("ALERT-CLOSE-UPDATER");
        this.alertVo = alertVo;
    }

    @Override
    protected void execute() {
        List<AlertVo> alertList = alertMapper.getAlertByParentId(alertVo.getId());
        if (CollectionUtils.isNotEmpty(alertList)) {
            for (AlertVo childAlertVo : alertList) {
                childAlertVo.setIsClose(alertVo.getIsClose());
                alertService.handleAlert(childAlertVo);
            }
        }
    }
}
