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
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertEventPluginVo;
import neatlogic.framework.alert.event.AlertEventHandlerFactory;
import neatlogic.framework.alert.event.IAlertEventHandler;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertEventPluginApi extends PrivateApiComponentBase {

    @Resource
    private AlertEventMapper alertEventMapper;

    @Override
    public String getToken() {
        return "alert/event/plugin/list";
    }

    @Override
    public String getName() {
        return "term.alert.event.listhandlerplugin";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "eventName", desc = "term.alert.event.eventname", type = ApiParamType.STRING),
            @Param(name = "parentPlugin", desc = "term.alert.event.parentplugin", type = ApiParamType.STRING)})
    @Output({@Param(explode = AlertEventPluginVo[].class)})
    @Description(desc = "term.alert.event.listhandlerplugin")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String eventName = jsonObj.getString("eventName");
        String parentPlugin = jsonObj.getString("parentPlugin");
        List<IAlertEventHandler> handlerList = AlertEventHandlerFactory.getHandlerList(eventName, parentPlugin);
        List<AlertEventPluginVo> pluginList = new ArrayList<>();
        handlerList.sort((o1, o2) -> o1.getSort() - o2.getSort());
        List<AlertEventPluginVo> allPluginList = alertEventMapper.getAllAlertEventPluginConfig();
        if (CollectionUtils.isNotEmpty(handlerList)) {
            for (IAlertEventHandler handler : handlerList) {
                AlertEventPluginVo plugin = new AlertEventPluginVo(handler.getName(), handler.getLabel(), handler.getIcon(), handler.getDescription());
                Optional<AlertEventPluginVo> op = allPluginList.stream().filter(o -> o.getName().equals(plugin.getName())).findFirst();
                if (op.isPresent()) {
                    plugin.setIsActive(op.get().getIsActive());
                    plugin.setConfigStr(op.get().getConfigStr());
                } else {
                    //没有任何配置默认激活插件
                    plugin.setIsActive(1);
                }
                plugin.setIsAsync(handler.isAsync() ? 1 : 0);
                pluginList.add(plugin);
            }
        }
        return pluginList;
    }
}
