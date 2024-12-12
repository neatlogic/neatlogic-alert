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

package neatlogic.module.alert.api.alertlevel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_LEVEL_MODIFY;
import neatlogic.framework.alert.dto.AlertLevelVo;
import neatlogic.framework.alert.exception.alertlevel.AlertLevelIsExistsException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertLevelMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_LEVEL_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveAlertLevelApi extends PrivateApiComponentBase {

    @Resource
    private AlertLevelMapper alertLevelMapper;


    @Override
    public String getToken() {
        return "alert/level/save";
    }

    @Override
    public String getName() {
        return "保存告警级别";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "level", desc = "告警级别", isRequired = true, type = ApiParamType.INTEGER),
            @Param(name = "name", desc = "唯一标识", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "label", desc = "名称", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "color", desc = "颜色", type = ApiParamType.STRING),
    })
    @Output({@Param(name = "id", desc = "id", type = ApiParamType.LONG)})
    @Description(desc = "保存告警级别")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertLevelVo alertLevelVo = JSON.toJavaObject(jsonObj, AlertLevelVo.class);
        if (alertLevelMapper.checkAlertLevelIsExists(alertLevelVo) > 0) {
            throw new AlertLevelIsExistsException(alertLevelVo.getLevel());
        }
        if (alertLevelMapper.checkAlertLevelNameIsExists(alertLevelVo) > 0) {
            throw new AlertLevelIsExistsException(alertLevelVo.getName());
        }
        alertLevelMapper.saveAlertLevel(alertLevelVo);
        return alertLevelVo.getId();
    }


}
