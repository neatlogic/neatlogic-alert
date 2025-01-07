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

package neatlogic.module.alert.api.alertaudit;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertEventHandlerAuditVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertEventAuditApi extends PrivateApiComponentBase {

    @Resource
    private AlertAuditMapper alertAuditMapper;


    @Override
    public String getToken() {
        return "/alert/event/audit/search";
    }

    @Override
    public String getName() {
        return "搜索告警事件记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "alertId", desc = "告警id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页大小", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = AlertEventHandlerAuditVo[].class)})
    @Description(desc = "搜索告警事件记录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertEventHandlerAuditVo alertEventHandlerAuditVo = JSON.toJavaObject(jsonObj, AlertEventHandlerAuditVo.class);
        List<AlertEventHandlerAuditVo> auditList = alertAuditMapper.searchAlertEventAudit(alertEventHandlerAuditVo);
        List<AlertEventHandlerAuditVo> rootAuditList = auditList.stream().filter(d -> d.getParentId() == null).collect(Collectors.toList());
        makeupChildAudit(rootAuditList, auditList);
        return rootAuditList;
    }

    private void makeupChildAudit(List<AlertEventHandlerAuditVo> parentAuditList, List<AlertEventHandlerAuditVo> allAuditList) {
        for (AlertEventHandlerAuditVo auditVo : parentAuditList) {
            List<AlertEventHandlerAuditVo> childAuditList = allAuditList.stream().filter(d -> d.getParentId() != null && d.getParentId().equals(auditVo.getId())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(childAuditList)) {
                auditVo.setChildAuditList(childAuditList);
                makeupChildAudit(childAuditList, allAuditList);
            }
        }
    }
}
