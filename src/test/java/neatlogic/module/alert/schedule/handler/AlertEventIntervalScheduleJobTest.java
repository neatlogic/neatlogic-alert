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

package neatlogic.module.alert.schedule.handler;

import org.junit.Assert;
import org.junit.Test;

public class AlertEventIntervalScheduleJobTest {

    /**
     * 验证服务重载旧分钟级作业时，缺失的秒字段按零处理。
     */
    @Test
    public void legacyJobUsesMinuteInterval() {
        Assert.assertEquals(300, AlertEventIntervalScheduleJob.calculateSeconds(5, null));
    }

    /**
     * 验证服务重载作业时会使用分钟和秒的完整间隔。
     */
    @Test
    public void reloadedJobUsesCompositeInterval() {
        Assert.assertEquals(90, AlertEventIntervalScheduleJob.calculateSeconds(1, 30));
        Assert.assertEquals(30, AlertEventIntervalScheduleJob.calculateSeconds(0, 30));
    }
}
