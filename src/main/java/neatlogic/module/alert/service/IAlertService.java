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

package neatlogic.module.alert.service;

import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertTrashVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IAlertService {

    boolean updateAlertStatus(AlertVo alertVo);

    boolean openAlert(AlertVo alertVo);

    boolean closeAlert(AlertVo alertVo);

    void deleteAlert(List<Long> alertIdList, boolean isDeleteChildAlert);

    @Transactional
    void deleteAlert(Long alertId, boolean isDeleteChildAlert);

    @Transactional
    void handleAlert(AlertVo alertVo);

    void saveOriginAlert(OriginalAlertVo originalAlertVo);

    @Transactional
    void saveAlert(AlertVo alertVo, boolean isSerial);

    List<AlertTrashVo> searchAlertTrash(AlertTrashVo alertTrashVo);

    long searchAlertCount(AlertVo alertVo);

    void saveAlertTrash(AlertTrashVo alertVo);

    List<AlertVo> searchAlert(AlertVo alertVo);

    List<OriginalAlertVo> searchOriginAlert(OriginalAlertVo originalAlertVo);

    List<AlertEventHandlerVo> listAlertEventHandler(AlertEventHandlerVo alertEventHandlerVo);
}
