/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
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
