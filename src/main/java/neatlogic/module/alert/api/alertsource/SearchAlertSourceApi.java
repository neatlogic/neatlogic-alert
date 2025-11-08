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

package neatlogic.module.alert.api.alertsource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertSourceVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertSourceMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertSourceApi extends PrivateApiComponentBase {

    @Resource
    private AlertSourceMapper alertSourceMapper;

    @Override
    public String getToken() {
        return "/alert/source/search";
    }

    @Override
    public String getName() {
        return "搜索告警来源";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页")
    })
    @Output({@Param(explode = AlertSourceVo[].class)})
    @Description(desc = "搜索告警来源")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertSourceVo alertSourceVo = JSON.toJavaObject(jsonObj, AlertSourceVo.class);
        List<AlertSourceVo> alertSourceList = alertSourceMapper.searchAlertSource(alertSourceVo);
        if (CollectionUtils.isNotEmpty(alertSourceList)) {
            int rowNum = alertSourceMapper.searchAlertSourceCount(alertSourceVo);
            alertSourceVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(alertSourceList, alertSourceVo);
    }
}
