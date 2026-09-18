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

import org.junit.Assert;
import org.junit.Test;

public class AlertIntervalEventHandlerTest {

    /**
     * 验证旧配置缺少秒字段时仍按原分钟数计算。
     */
    @Test
    public void minuteOnlyConfigKeepsOriginalDuration() {
        Assert.assertEquals(120, AlertIntervalEventHandler.calculateSeconds(2, null));
    }

    /**
     * 验证纯秒配置和分钟加秒配置都能转换为总秒数。
     */
    @Test
    public void secondAndCompositeDurationsAreSupported() {
        Assert.assertEquals(30, AlertIntervalEventHandler.calculateSeconds(0, 30));
        Assert.assertEquals(90, AlertIntervalEventHandler.calculateSeconds(1, 30));
    }

    /**
     * 验证零延时会立即执行，并只将重复次数交给调度器。
     */
    @Test
    public void zeroDelayKeepsRepeatCountSemantics() {
        Assert.assertEquals(3, AlertIntervalEventHandler.calculateLeftExecuteCount(0, 30, 3));
        Assert.assertEquals(0, AlertIntervalEventHandler.calculateLeftExecuteCount(0, 0, 3));
    }

    /**
     * 验证存在首次延时时，调度器执行次数包含首次延时执行。
     */
    @Test
    public void delayedExecutionIncludesFirstScheduledRun() {
        Assert.assertEquals(4, AlertIntervalEventHandler.calculateLeftExecuteCount(30, 90, 3));
        Assert.assertEquals(1, AlertIntervalEventHandler.calculateLeftExecuteCount(30, 0, 3));
    }
}
