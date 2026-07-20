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

package neatlogic.module.alert.api.alertaudit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditSearchVo;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.service.AlertEventAuditService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchRootAlertEventAuditApi extends PrivateApiComponentBase {

    @Resource
    private AlertAuditMapper alertAuditMapper;

    @Resource
    private AlertEventAuditService alertEventAuditService;

    @Override
    public String getToken() {
        return "/alert/event/audit/root/search";
    }

    @Override
    public String getName() {
        return "搜索告警事件执行根记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "auditId", desc = "审计记录ID", type = ApiParamType.LONG),
            @Param(name = "alertId", desc = "告警ID", type = ApiParamType.LONG),
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            @Param(name = "event", desc = "事件", type = ApiParamType.STRING),
            @Param(name = "handler", desc = "根插件", type = ApiParamType.STRING),
            @Param(name = "status", desc = "根记录状态", type = ApiParamType.STRING),
            @Param(name = "timeRange", desc = "根记录执行时间范围", type = ApiParamType.JSONARRAY),
            @Param(name = "hasChild", desc = "是否存在子记录", type = ApiParamType.INTEGER),
            @Param(name = "currentPage", desc = "common.currentpage", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "common.pagesize", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = AlertEventHandlerAuditVo[].class)})
    @Description(desc = "搜索告警事件执行根记录")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertEventHandlerAuditSearchVo searchVo = JSON.toJavaObject(jsonObj, AlertEventHandlerAuditSearchVo.class);
        // 审计ID允许指向任意层级，分页前统一换算为所属根记录ID。
        if (searchVo.getAuditId() != null) {
            AlertEventHandlerAuditVo rootAudit = alertEventAuditService.getRootAudit(searchVo.getAuditId());
            if (rootAudit == null) {
                List<AlertEventHandlerAuditVo> emptyList = new ArrayList<>();
                return TableResultUtil.getResult(emptyList, searchVo);
            }
            searchVo.setRootAuditId(rootAudit.getId());
        }
        int rowNum = alertAuditMapper.searchRootAlertEventAuditCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(alertAuditMapper.searchRootAlertEventAudit(searchVo), searchVo);
    }
}
