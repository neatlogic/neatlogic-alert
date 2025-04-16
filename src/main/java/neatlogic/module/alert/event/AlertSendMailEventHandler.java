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
import neatlogic.framework.alert.enums.AlertEventStatus;
import neatlogic.framework.alert.enums.AlertUserType;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.common.constvalue.AuthType;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.util.EmailUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class AlertSendMailEventHandler extends AlertEventHandlerBase {
    //private final Logger logger = LoggerFactory.getLogger(AlertSendMailEventHandler.class);
    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public int getSort() {
        return 3;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (alertEventPluginVo != null && MapUtils.isNotEmpty(alertEventPluginVo.getConfig())) {
            //检查是否到达最大发送次数
            JSONObject pluginConfig = alertEventPluginVo.getConfig();
            int maxSendCount = pluginConfig.getIntValue("maxSendCount");
            if (maxSendCount > 0 && alertEventHandlerAuditVo.getStartTime() != null) {
                Integer c = alertEventMapper.getAlertEventExecuteCount(alertEventHandlerVo.getHandler(), alertEventHandlerAuditVo.getStartTime().getTime() - 60 * 1000);
                if (c != null && c > maxSendCount) {
                    throw new ApiRuntimeException("已经到达一分钟内最大发送次数：" + maxSendCount);
                }
            }
        }
        if (MapUtils.isNotEmpty(config)) {
            List<AlertAttrDefineVo> attrList = AlertAttr.getConstAttrList(1);
            int interval = config.getIntValue("interval");
            if (interval > 0) {
                AlertEventHandlerAuditVo paramAuditVo = new AlertEventHandlerAuditVo();
                paramAuditVo.setAlertId(alertVo.getId());
                paramAuditVo.setEventHandlerId(alertEventHandlerVo.getId());
                paramAuditVo.setStatus(AlertEventStatus.SUCCEED.getValue());
                AlertEventHandlerAuditVo auditVo = alertEventMapper.getLastAlertEventHandlerAudit(paramAuditVo);
                if (auditVo != null) {
                    Date now = new Date();
                    long diff = now.getTime() - auditVo.getStartTime().getTime();
                    if (diff < (long) interval * 60 * 1000) {
                        //如果时间不够间隔，直接返回告警
                        alertEventStatusVo.setSkipped(true);
                        //config.put("error", "离上次成功发送过去了" + (diff / 1000) + "秒，未到间隔时间，发送跳过");
                        alertEventHandlerAuditVo.setResult(config);
                        return alertVo;
                    }
                }
            }
            JSONArray toUserList = config.getJSONArray("toUserList");
            JSONArray ccUserList = config.getJSONArray("ccUserList");
            String title = config.getString("title");
            String content = config.getString("content");
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
            title = FreemarkerUtil.transform(paramObj, title);
            content = FreemarkerUtil.transform(paramObj, content);

            List<AlertUserVo> userList = alertVo.getUserList();
            Set<String> to = new HashSet<>();
            if (CollectionUtils.isNotEmpty(toUserList)) {
                for (int i = 0; i < toUserList.size(); i++) {
                    String userUuid = toUserList.getString(i);
                    if (("alertUserType#" + AlertUserType.WORKER.getValue()).equals(userUuid)) {
                        if (CollectionUtils.isNotEmpty(userList)) {
                            for (AlertUserVo user : userList) {
                                if (StringUtils.isNotBlank(user.getUserEmail())) {
                                    to.add(user.getUserEmail());
                                }
                            }
                        }
                    } else {
                        userUuid = AuthType.removePrefix(userUuid);
                    }
                    UserVo userVo = userMapper.getUserByUuid(userUuid);
                    if (userVo != null && StringUtils.isNotBlank(userVo.getEmail())) {
                        to.add(userVo.getEmail());
                    }
                }
            }
            Set<String> cc = new HashSet<>();
            if (CollectionUtils.isNotEmpty(ccUserList)) {
                for (int i = 0; i < ccUserList.size(); i++) {
                    String userUuid = ccUserList.getString(i);
                    if (("alertUserType#" + AlertUserType.WORKER.getValue()).equals(userUuid)) {
                        if (CollectionUtils.isNotEmpty(userList)) {
                            for (AlertUserVo user : userList) {
                                if (StringUtils.isNotBlank(user.getUserEmail())) {
                                    to.add(user.getUserEmail());
                                }
                            }
                        }
                    } else {
                        userUuid = AuthType.removePrefix(userUuid);
                    }
                    UserVo userVo = userMapper.getUserByUuid(userUuid);
                    if (userVo != null && StringUtils.isNotBlank(userVo.getEmail())) {
                        cc.add(userVo.getEmail());
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(to) || CollectionUtils.isNotEmpty(cc)) {
                try {
                    EmailUtil.sendHtmlEmail(title, content, new ArrayList<>(to), new ArrayList<>(cc));
                } catch (Exception ex) {
                    throw new AlertEventHandlerTriggerException(ex);
                }
            }
        }
        return alertVo;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public String getName() {
        return "EMAIL";
    }

    @Override
    public String getLabel() {
        return "发送邮件";
    }

    @Override
    public String getIcon() {
        return "tsfont-mail-o";
    }

    @Override
    public String getDescription() {
        return "用smtp方式发送邮件，需要到系统管理中配置邮件服务器信息，收件用户邮箱地址不能为空。";
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
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
        }};
    }

    @Override
    public List<AlertEventHandlerConfigVo> getHandlerConfig(AlertEventHandlerVo alertEventHandlerVo) {
        List<AlertEventHandlerConfigVo> configList = new ArrayList<>();
        AlertEventHandlerConfigVo alertEventHandlerConfigVo = new AlertEventHandlerConfigVo();
        alertEventHandlerConfigVo.setAlertEventHandlerId(alertEventHandlerVo.getId());
        alertEventHandlerConfigVo.setUuid(alertEventHandlerVo.getUuid());
        alertEventHandlerConfigVo.setHandler(alertEventHandlerVo.getHandler());
        alertEventHandlerConfigVo.setConfig(alertEventHandlerVo.getConfig());
        configList.add(alertEventHandlerConfigVo);
        return configList;
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
        }};
    }
}
