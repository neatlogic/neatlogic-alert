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

package neatlogic.module.alert.startup.handler;

import neatlogic.framework.startup.StartupBase;
import neatlogic.module.alert.service.AlertDeleteHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RecoverAlertDeleteTaskStartupHandler extends StartupBase {
    @Resource
    private AlertDeleteHandler alertDeleteHandler;

    @Override
    public String getName() {
        return "恢复告警删除任务";
    }

    @Override
    public int sort() {
        return 5;
    }

    @Override
    public int executeForCurrentTenant() {
        // 主表中的删除标记是持久化恢复依据，服务重启后按租户重新提交未完成任务。
        alertDeleteHandler.recoverPendingDeleteTask();
        return 0;
    }
}
