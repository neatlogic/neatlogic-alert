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

package neatlogic.module.alert.dao.mapper;

import neatlogic.framework.alert.dto.AlertSourceVo;

import java.util.List;

public interface AlertSourceMapper {
    int checkAlertSourceIsExists(AlertSourceVo alertSourceVo);

    List<AlertSourceVo> searchAlertSource(AlertSourceVo vo);

    AlertSourceVo getAlertSourceByName(String name);

    int searchAlertSourceCount(AlertSourceVo vo);

    void saveAlertSource(AlertSourceVo alertSourceVo);

    void deleteAlertSourceByName(String name);
}
