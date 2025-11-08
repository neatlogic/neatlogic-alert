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
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.service.IAlertService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchOriginalAlertApi extends PrivateApiComponentBase {

    //@Resource
    //private AlertMapper alertMapper;

    @Resource
    private IAlertService alertService;

    @Override
    public String getToken() {
        return "/alert/origin/search";
    }

    @Override
    public String getName() {
        return "搜索接入记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            @Param(name = "status", desc = "状态", rule = "succeed,failed", type = ApiParamType.STRING),
            @Param(name = "timeRange", desc = "时间范围", type = ApiParamType.JSONARRAY)
    })
    @Description(desc = "搜索接入记录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        OriginalAlertVo originalAlertVo = JSON.toJavaObject(jsonObj, OriginalAlertVo.class);
        List<OriginalAlertVo> alertList = alertService.searchOriginAlert(originalAlertVo);
//        if (CollectionUtils.isNotEmpty(alertList)) {
//            int rowNum = alertMapper.searchAlertOriginCount(originalAlertVo);
//            originalAlertVo.setRowNum(rowNum);
//        }
        return TableResultUtil.getResult(alertList, originalAlertVo);
    }
}
