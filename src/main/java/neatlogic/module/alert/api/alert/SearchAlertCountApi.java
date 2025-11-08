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

package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.service.IAlertService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertCountApi extends PrivateApiComponentBase {

    @Resource
    private IAlertService alertService;


    @Override
    public String getToken() {
        return "/alert/search/count";
    }

    @Override
    public String getName() {
        return "搜索告警数量";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "viewName", desc = "视图", type = ApiParamType.STRING)
    })
    @Description(desc = "搜索告警数量")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertVo alertVo = JSON.toJavaObject(jsonObj, AlertVo.class);
        return alertService.searchAlertCount(alertVo);
    }
}
