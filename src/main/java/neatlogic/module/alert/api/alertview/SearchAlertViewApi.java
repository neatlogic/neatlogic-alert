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

package neatlogic.module.alert.api.alertview;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertViewApi extends PrivateApiComponentBase {

    @Resource
    private AlertViewMapper alertViewMapper;

    @Override
    public String getToken() {
        return "/alert/view/search";
    }

    @Override
    public String getName() {
        return "返回告警视图列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage")
    })
    @Output({@Param(explode = AlertViewVo[].class)})
    @Description(desc = "返回告警视图列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertViewVo alertViewVo = JSON.toJavaObject(jsonObj, AlertViewVo.class);
        if (AuthActionChecker.check(ALERT_VIEW_MODIFY.class)) {
            alertViewVo.setAdmin(true);
        } else {
            alertViewVo.setIsActive(1);
        }
        int rowNum = alertViewMapper.searchAlertViewCount(alertViewVo);
        if (rowNum > 0) {
            alertViewVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(alertViewMapper.searchAlertView(alertViewVo), alertViewVo);
    }

}
