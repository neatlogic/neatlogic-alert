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

package neatlogic.module.alert.api.alertevent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_EVENT_MODIFY;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
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
        //先删除所有子组件
        AlertEventHandlerVo param = new AlertEventHandlerVo();
        param.setParentId(alertEventHandlerVo.getId());
        List<AlertEventHandlerVo> subHandlerList = alertEventMapper.listEventHandler(param);
        if (CollectionUtils.isNotEmpty(subHandlerList)) {
            for (AlertEventHandlerVo subHandler : subHandlerList) {
                alertEventMapper.deleteAlertEventHandlerById(subHandler.getId());
            }
        }
        Integer sort = alertEventMapper.getAlertEventHandlerMaxSort(alertEventHandlerVo);
        if (sort == null) {
            sort = 0;
        }
        sort++;
        alertEventHandlerVo.setSort(sort);
        alertEventMapper.saveAlertEventHandler(alertEventHandlerVo);
        saveSubHandler(alertEventHandlerVo.getId(), alertEventHandlerVo);
        return alertEventHandlerVo.getId();
    }

    private void saveSubHandler(Long parentId, AlertEventHandlerVo alertEventHandlerVo) {
        if (CollectionUtils.isNotEmpty(alertEventHandlerVo.getHandlerList())) {
            int sort = 1;
            for (AlertEventHandlerVo handlerVo : alertEventHandlerVo.getHandlerList()) {
                handlerVo.setParentId(parentId);
                handlerVo.setSort(sort);
                alertEventMapper.saveAlertEventHandler(handlerVo);
                saveSubHandler(handlerVo.getId(), handlerVo);
                sort += 1;
            }
        }
    }
}
