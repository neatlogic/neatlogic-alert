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

package neatlogic.module.alert.api.alerttype;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertTypeVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.label.MQ_MODIFY;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertTypeMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@AuthAction(action = MQ_MODIFY.class)//消息队列管理页面添加订阅操作会调用该接口
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertTypeApi extends PrivateApiComponentBase {

    @Resource
    private AlertTypeMapper alertTypeMapper;

    @Override
    public String getToken() {
        return "alert/alerttype/search";
    }

    @Override
    public String getName() {
        return "搜索告警类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            //@Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            //@Param(name = "pageSize", desc = "每页大小", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(explode = AlertTypeVo[].class)
    })
    @Description(desc = "搜索告警类型")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertTypeVo alertTypeVo = JSON.toJavaObject(jsonObj, AlertTypeVo.class);
        //int rowNum = alertTypeMapper.searchAlertTypeCount(alertTypeVo);
        List<AlertTypeVo> alertTypeList = null;
        //if (rowNum > 0) {
        //alertTypeVo.setRowNum(rowNum);
        alertTypeList = alertTypeMapper.searchAlertType(alertTypeVo);
        //}
        return TableResultUtil.getResult(alertTypeList, alertTypeVo);
    }

}
