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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_LEVEL_MODIFY;
import neatlogic.framework.alert.dto.AlertLevelVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertLevelMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_LEVEL_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeleteAlertLevelApi extends PrivateApiComponentBase {

    @Resource
    private AlertLevelMapper alertLevelMapper;

    @Override
    public String getToken() {
        return "/alert/level/delete";
    }

    @Override
    public String getName() {
        return "删除告警级别";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "id")})
    @Output({@Param(explode = AlertLevelVo.class)})
    @Description(desc = "删除告警级别")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        Long id = jsonObj.getLong("id");
        alertLevelMapper.deleteAlertLevel(id);
        return null;
    }

}
