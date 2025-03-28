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

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.exception.integration.IntegrationHandlerNotFoundException;
import neatlogic.framework.exception.integration.IntegrationNotFoundException;
import neatlogic.framework.exception.integration.IntegrationUnActiveException;
import neatlogic.framework.integration.core.IIntegrationHandler;
import neatlogic.framework.integration.core.IntegrationHandlerFactory;
import neatlogic.framework.integration.dao.mapper.IntegrationMapper;
import neatlogic.framework.integration.dto.IntegrationResultVo;
import neatlogic.framework.integration.dto.IntegrationVo;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.framework.integration.handler.FrameworkRequestFrom;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class AlertIntegrationEventHandler extends AlertEventHandlerBase {
    private final Logger logger = LoggerFactory.getLogger(AlertIntegrationEventHandler.class);
    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private IntegrationMapper integrationMapper;

    @Override
    public int getSort() {
        return 6;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        JSONObject resultObj = new JSONObject();
        if (config == null) {
            config = new JSONObject();
        }
        String integrationUuid = config.getString("integrationUuid");
        JSONArray paramMapping = config.getJSONArray("paramMapping");
        if (StringUtils.isNotBlank(integrationUuid)) {
            //补充处理人和处理组信息
            alertVo.setUserList(alertMapper.getAlertUserByAlertId(alertVo.getId()));
            alertVo.setTeamList(alertMapper.getAlertTeamByAlertId(alertVo.getId()));


            IntegrationVo integrationVo = integrationMapper.getIntegrationByUuid(integrationUuid);
            if (integrationVo == null) {
                throw new IntegrationNotFoundException(integrationUuid);
            }
            if (integrationVo.getIsActive() != 1) {
                throw new IntegrationUnActiveException(integrationUuid);
            }
            IIntegrationHandler handler = IntegrationHandlerFactory.getHandler(integrationVo.getHandler());
            if (handler == null) {
                throw new IntegrationHandlerNotFoundException(integrationVo.getHandler());
            }
            List<AlertAttrDefineVo> attrList = AlertAttr.getConstAttrList(1);
            JSONObject integrationParam = new JSONObject();
            //获取集成的所有入参
            Map<String, String> paramTypeMap = new HashMap<>();
            if (integrationVo.getConfig().getJSONObject("param") != null) {
                if (integrationVo.getConfig().getJSONObject("param").getJSONArray("paramList") != null) {
                    for (int i = 0; i < integrationVo.getConfig().getJSONObject("param").getJSONArray("paramList").size(); i++) {
                        JSONObject paramObj = integrationVo.getConfig().getJSONObject("param").getJSONArray("paramList").getJSONObject(i);
                        if (paramObj.getString("mode").equals("input")) {
                            paramTypeMap.put(paramObj.getString("name"), paramObj.getString("type"));
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(paramMapping)) {
                JSONObject paramObj = new JSONObject();
                JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
                for (AlertAttrDefineVo attr : attrList) {
                    paramObj.put(attr.getName(), alertObj.get(attr.getName().replace("const_", "")));
                }
                if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
                    List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
                    for (AlertAttrTypeVo alertAttr : attrTypeList) {
                        paramObj.put("attr_" + alertAttr.getName(), alertVo.getAttrObj().get(alertAttr.getName()));
                    }
                }
                for (int i = 0; i < paramMapping.size(); i++) {
                    JSONObject mapping = paramMapping.getJSONObject(i);
                    //尝试把转换好的数据转换成对象或数组，不行才当字符串处理
                    String transferred = FreemarkerUtil.transform(paramObj, mapping.getString("expression"));
                    if (paramTypeMap.containsKey(mapping.getString("name")) && paramTypeMap.get(mapping.getString("name")).equalsIgnoreCase("array")) {
                        try {
                            JSONArray list = JSON.parseArray(transferred);
                            integrationParam.put(mapping.getString("name"), list);
                        } catch (Exception e2) {
                            integrationParam.put(mapping.getString("name"), transferred);
                        }
                    } else {
                        integrationParam.put(mapping.getString("name"), transferred);
                    }
                }
            }
            integrationVo.setParamObj(integrationParam);
            IntegrationResultVo resultVo = handler.sendRequest(integrationVo, FrameworkRequestFrom.API);
            String resultJson = resultVo.getTransformedResult();
            if (StringUtils.isBlank(resultJson)) {
                resultJson = resultVo.getRawResult();
            }
            resultObj.put("response", resultJson);
        }
        alertEventHandlerAuditVo.setResult(resultObj);
        return alertVo;
    }


    @Override
    public String getName() {
        return "INTEGRATION";
    }

    @Override
    public String getLabel() {
        return "调用集成";
    }

    @Override
    public String getIcon() {
        return "tsfont-action";
    }

    @Override
    public String getDescription() {
        return "用于调用第三方系统的restful接口。";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_DELETE.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
        }};
    }

}
