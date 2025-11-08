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
