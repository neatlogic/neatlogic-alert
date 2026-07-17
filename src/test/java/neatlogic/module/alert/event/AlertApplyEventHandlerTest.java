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

import com.alibaba.fastjson.JSONArray;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AlertApplyEventHandlerTest {

    /**
     * 验证APPLY追加新处理对象时会保留数据库中的已有对象。
     */
    @Test
    public void mergeAssignmentKeepsExistingIds() {
        List<String> result = AlertApplyEventHandler.mergeAssignmentIdList(
                Arrays.asList("old-user-1", "old-user-2"),
                arrayOf("new-user")
        );

        Assert.assertEquals(Arrays.asList("old-user-1", "old-user-2", "new-user"), result);
    }

    /**
     * 验证只配置处理人时，空处理组配置不会清空已有处理组。
     */
    @Test
    public void emptyApplyListKeepsExistingIds() {
        List<String> result = AlertApplyEventHandler.mergeAssignmentIdList(
                Arrays.asList("old-team-1", "old-team-2"),
                new JSONArray()
        );

        Assert.assertEquals(Arrays.asList("old-team-1", "old-team-2"), result);
    }

    /**
     * 验证重复配置不会在最终处理对象列表中产生重复值。
     */
    @Test
    public void duplicateAssignmentIdsAreRemoved() {
        List<String> result = AlertApplyEventHandler.mergeAssignmentIdList(
                Collections.singletonList("old-user"),
                arrayOf("old-user", "new-user", "new-user")
        );

        Assert.assertEquals(Arrays.asList("old-user", "new-user"), result);
    }

    /**
     * 构造测试使用的JSON数组。
     */
    private JSONArray arrayOf(String... values) {
        JSONArray result = new JSONArray();
        result.addAll(Arrays.asList(values));
        return result;
    }
}
