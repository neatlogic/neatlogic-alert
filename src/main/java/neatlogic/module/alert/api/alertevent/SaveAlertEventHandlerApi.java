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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_EVENT_MODIFY;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.breaker.AlertEventHandlerBreakerPolicyVo;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.event.IAlertEventHandler;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerNotFoundException;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerNotSupportException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = ALERT_EVENT_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertEventHandlerApi extends PrivateApiComponentBase {


    @Resource
    private AlertEventMapper alertEventMapper;
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "alert/event/handler/save";
    }

    @Override
    public String getName() {
        return "保存告警事件插件配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "uuid", desc = "uuid", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "alertType", desc = "告警类型", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "parentId", desc = "父组件id", type = ApiParamType.LONG),
            @Param(name = "parentUuid", desc = "父组件uuid", type = ApiParamType.STRING),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.INTEGER, isRequired = true),
            @Param(name = "name", desc = "名称", isRequired = true, type = ApiParamType.STRING, maxLength = 50),
            @Param(name = "event", desc = "事件", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "handler", desc = "插件", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "config", desc = "配置", type = ApiParamType.JSONOBJECT),
            @Param(name = "breakerPolicyList", desc = "熔断策略列表", type = ApiParamType.JSONARRAY),
    })
    @Output({@Param(explode = AlertEventHandlerVo[].class)})
    @Description(desc = "保存告警事件插件配置")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertEventHandlerVo alertEventHandlerVo = JSON.toJavaObject(jsonObj, AlertEventHandlerVo.class);
        IAlertEventHandler handler = AlertEventHandlerFactory.getHandler(alertEventHandlerVo.getHandler());
        if (handler == null) {
            throw new AlertEventHandlerNotFoundException(alertEventHandlerVo.getEvent());
        }

        if (!handler.supportEventTypes().contains(alertEventHandlerVo.getEvent())) {
            throw new AlertEventHandlerNotSupportException(handler.getLabel(), alertEventHandlerVo.getEvent());
        }

        Integer sort = alertEventMapper.getAlertEventHandlerMaxSort(alertEventHandlerVo);
        if (sort == null) {
            sort = 0;
        }
        sort++;
        alertEventHandlerVo.setSort(sort);
        handler.makeupChildHandler(alertEventHandlerVo);
        alertEventMapper.saveAlertEventHandler(alertEventHandlerVo);
        saveBreakerPolicy(alertEventHandlerVo);
        saveSubHandler(alertEventHandlerVo.getId(), alertEventHandlerVo);
        return alertEventHandlerVo.getId();
    }

    private void saveBreakerPolicy(AlertEventHandlerVo alertEventHandlerVo) {
        alertBreakerMapper.deleteEventHandlerBreakerPolicyByEventHandlerId(alertEventHandlerVo.getId());
        if (CollectionUtils.isNotEmpty(alertEventHandlerVo.getBreakerPolicyList())) {
            int sort = 1;
            for (AlertEventHandlerBreakerPolicyVo policyVo : alertEventHandlerVo.getBreakerPolicyList()) {
                policyVo.setEventHandlerId(alertEventHandlerVo.getId());
                policyVo.setSort(sort);
                alertBreakerMapper.insertEventHandlerBreakerPolicy(policyVo);
                sort += 1;
            }
        }
    }

    private void saveSubHandler(Long parentId, AlertEventHandlerVo alertEventHandlerVo) {
        //只获取直系子handler
        List<AlertEventHandlerVo> subHandlerList = alertEventMapper.getAlertEventHandlerByParentId(alertEventHandlerVo.getId());

        if (CollectionUtils.isNotEmpty(alertEventHandlerVo.getHandlerList())) {
            //根据uuid过滤出需要删除的直系子handler
            if (CollectionUtils.isNotEmpty(subHandlerList)) {
                subHandlerList.removeAll(alertEventHandlerVo.getHandlerList());
            }
            int sort = 1;
            for (AlertEventHandlerVo handlerVo : alertEventHandlerVo.getHandlerList()) {
                AlertEventHandlerVo oldHandlerVo = alertEventMapper.getAlertEventHandlerByUuid(handlerVo.getUuid());
                if(oldHandlerVo!=null){
                    handlerVo.setId(oldHandlerVo.getId());
                }
                handlerVo.setParentId(parentId);
                handlerVo.setSort(sort);
                alertEventMapper.saveAlertEventHandler(handlerVo);
                saveBreakerPolicy(handlerVo);
                saveSubHandler(handlerVo.getId(), handlerVo);
                sort += 1;
            }
        }

        //删除没有用的子组件
        if (CollectionUtils.isNotEmpty(subHandlerList)) {
            for (AlertEventHandlerVo subHandler : subHandlerList) {
                AlertEventHandlerVo param = new AlertEventHandlerVo();
                param.setParentId(subHandler.getId());
                //级联查出所有子模块再删除
                List<AlertEventHandlerVo> subSubHandlerList = alertEventMapper.listEventHandler(param);
                if (CollectionUtils.isNotEmpty(subSubHandlerList)) {
                    for (AlertEventHandlerVo subSubHandler : subSubHandlerList) {
                        alertBreakerMapper.deleteEventHandlerBreakerPolicyByEventHandlerId(subSubHandler.getId());
                        alertEventMapper.deleteAlertEventHandlerById(subSubHandler.getId());
                    }
                }
                alertBreakerMapper.deleteEventHandlerBreakerPolicyByEventHandlerId(subHandler.getId());
                alertEventMapper.deleteAlertEventHandlerById(subHandler.getId());
            }
        }
    }
}
