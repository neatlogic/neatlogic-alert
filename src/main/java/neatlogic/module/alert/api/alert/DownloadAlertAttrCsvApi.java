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
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.NoAuth;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Service
@AuthAction(action = NoAuth.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class DownloadAlertAttrCsvApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private AlertMapper alertMapper;

    @Override
    public String getToken() {
        return "alert/attr/csv/download";
    }

    @Override
    public String getName() {
        return "下载告警CSV属性";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_WITHOUT_ENCRYPTION;
    }

    @Input({
            @Param(name = "alertId", desc = "告警id", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "attrName", desc = "属性唯一标识", type = ApiParamType.STRING, isRequired = true)
    })
    @Description(desc = "下载告警CSV属性")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Long alertId = jsonObj.getLong("alertId");
        String attrName = jsonObj.getString("attrName");
        AlertVo alertVo = alertMapper.getAlertById(alertId);
        if (alertVo == null) {
            throw new AlertNotFoundException(alertId);
        }
        Object value = null;
        if (alertVo.getAttrObj() != null) {
            value = alertVo.getAttrObj().get(attrName);
        }
        String csvContent;
        if (value == null) {
            csvContent = "";
        } else if (value instanceof JSONObject) {
            csvContent = JSON.toJSONString(value);
        } else {
            csvContent = value.toString();
        }
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"data.csv\"");
        response.getOutputStream().write(("\uFEFF" + csvContent).getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
        return null;
    }
}
