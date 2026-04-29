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

package neatlogic.module.alert.api.alertevent;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_EVENT_MODIFY;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_EVENT_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteAlertEventHandlerApi extends PrivateApiComponentBase {
    @Resource
    private AlertEventMapper alertEventMapper;
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "alert/event/handler/delete";
    }

    @Override
    public String getName() {
        return "删除事件组件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG, isRequired = true)
    })
    @Description(desc = "删除事件组件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        List<AlertEventHandlerVo> handlerList = new ArrayList<>();
        getAllChildren(id, handlerList);
        if (CollectionUtils.isNotEmpty(handlerList)) {
            for (AlertEventHandlerVo handler : handlerList) {
                alertBreakerMapper.deleteEventHandlerBreakerPolicyByEventHandlerId(handler.getId());
                alertEventMapper.deleteAlertEventHandlerById(handler.getId());
            }
        }
        alertBreakerMapper.deleteEventHandlerBreakerPolicyByEventHandlerId(id);
        alertEventMapper.deleteAlertEventHandlerById(id);
        return null;
    }

    private void getAllChildren(Long parentId, List<AlertEventHandlerVo> handlerList) {
        List<AlertEventHandlerVo> childList = alertEventMapper.getAlertEventHandlerByParentId(parentId);
        if (CollectionUtils.isNotEmpty(childList)) {
            handlerList.addAll(childList);
            for (AlertEventHandlerVo child : childList) {
                getAllChildren(child.getId(), handlerList);
            }
        }
    }
}
