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

import com.alibaba.fastjson.JSONObject;

import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertTrashVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IAlertService {

    boolean updateAlertStatus(AlertVo alertVo);

    boolean openAlert(AlertVo alertVo);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    boolean closeAlert(AlertVo alertVo);

    void deleteAlert(List<Long> alertIdList, boolean isDeleteChildAlert);

    @Transactional
    void deleteAlert(Long alertId, boolean isDeleteChildAlert);

    @Transactional
    void handleAlert(AlertVo alertVo);

    /** 校验全部目标权限后逐条修改标签，返回修改、不变、失效和失败数量。 */
    JSONObject batchUpdateAlertMark(List<Long> idList, List<String> markNameList, String mode);

    /** 独立修改一条告警标签，true表示修改、false表示不变、null表示已失效。 */
    @Transactional
    Boolean updateAlertMarkOnly(Long id, List<String> markNameList, String mode);

    /** 提交管理员匹配标签修改任务，后台使用固定搜索快照遍历目标。 */
    void submitMatchedAlertMarkUpdate(AlertVo searchVo, List<String> markNameList, String mode);

    void saveOriginAlert(OriginalAlertVo originalAlertVo);

    @Transactional
    void saveAlert(AlertVo alertVo, boolean isSerial);

    List<AlertTrashVo> searchAlertTrash(AlertTrashVo alertTrashVo);

    long searchAlertCount(AlertVo alertVo);

    void saveAlertTrash(AlertTrashVo alertVo);

    /**
     * 在独立事务中保存告警垃圾箱数据并删除告警主数据。
     *
     * @param alertTrashVo 告警垃圾箱数据
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void archiveAndDeleteAlert(AlertTrashVo alertTrashVo);

    List<AlertVo> searchAlert(AlertVo alertVo);

    List<OriginalAlertVo> searchOriginAlert(OriginalAlertVo originalAlertVo);

    List<AlertEventHandlerVo> listAlertEventHandler(AlertEventHandlerVo alertEventHandlerVo);
}
