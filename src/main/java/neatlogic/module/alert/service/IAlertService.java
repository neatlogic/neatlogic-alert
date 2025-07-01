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

package neatlogic.module.alert.service;

import neatlogic.framework.alert.dto.AlertEventHandlerVo;
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
    void saveAlert(AlertVo alertVo);

    long searchAlertCount(AlertVo alertVo);

    List<AlertVo> searchAlert(AlertVo alertVo);

    List<OriginalAlertVo> searchOriginAlert(OriginalAlertVo originalAlertVo);

    List<AlertEventHandlerVo> listAlertEventHandler(AlertEventHandlerVo alertEventHandlerVo);
}
