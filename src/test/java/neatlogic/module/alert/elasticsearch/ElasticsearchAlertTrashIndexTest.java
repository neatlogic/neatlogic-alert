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

package neatlogic.module.alert.elasticsearch;

import neatlogic.module.alert.dao.mapper.AlertTrashMapper;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public class ElasticsearchAlertTrashIndexTest {

    /**
     * 验证数据库中不存在目标告警时，单条索引重建直接结束而不抛出空指针异常。
     */
    @Test
    public void missingAlertTrashDoesNotCreateDocument() throws Exception {
        ElasticsearchAlertTrashIndex alertTrashIndex = new ElasticsearchAlertTrashIndex();
        AlertTrashMapper alertTrashMapper = (AlertTrashMapper) Proxy.newProxyInstance(
                AlertTrashMapper.class.getClassLoader(),
                new Class<?>[]{AlertTrashMapper.class},
                (proxy, method, args) -> null
        );
        Field mapperField = ElasticsearchAlertTrashIndex.class.getDeclaredField("alertTrashMapper");
        mapperField.setAccessible(true);
        mapperField.set(alertTrashIndex, alertTrashMapper);

        alertTrashIndex.myCreateDocument(123L);
    }
}
