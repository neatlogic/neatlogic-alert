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
import neatlogic.framework.alert.exception.alertnotifytemplate.NotifyTemplateIsUnActiveException;
import neatlogic.framework.alert.exception.alertnotifytemplate.NotifyTemplateNameIsNotFoundException;
import neatlogic.framework.common.constvalue.AuthType;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.TeamVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.util.EmailUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertNotifyTemplateMapper;
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
    private AlertNotifyTemplateMapper alertNotifyTemplateMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;


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
            Long mailServerId = config.getLong("mailServerId");
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
            String type = config.getString("type");
            String title;
            String content;
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
            if (StringUtils.isBlank(type) || !type.equalsIgnoreCase("template")) {
                title = config.getString("title");
                content = config.getString("content");
            } else {
                Long templateId = config.getLong("template");
                if (templateId == null) {
                    throw new ParamNotExistsException("模板");
                }
                AlertNotifyTemplateVo templateVo = alertNotifyTemplateMapper.getNotifyTemplateById(templateId);
                if (templateVo == null) {
                    throw new NotifyTemplateNameIsNotFoundException(templateId);
                }
                if (Objects.equals(templateVo.getIsActive(), 0)) {
                    throw new NotifyTemplateIsUnActiveException(templateVo.getName());
                }
                title = templateVo.getTitle();
                content = templateVo.getContent();
            }
            title = FreemarkerUtil.transform(paramObj, title);
            content = FreemarkerUtil.transform(paramObj, content);


            Set<String> to = makeupMailList(alertVo, toUserList);

            Set<String> cc = makeupMailList(alertVo, ccUserList);

            if (CollectionUtils.isNotEmpty(to) || CollectionUtils.isNotEmpty(cc)) {
                try {
                    EmailUtil.sendHtmlEmail(mailServerId, title, content, new ArrayList<>(to), new ArrayList<>(cc));
                } catch (Exception ex) {
                    throw new AlertEventHandlerTriggerException(ex);
                }
            }
        }
        return alertVo;
    }

    private Set<String> makeupMailList(AlertVo alertVo, JSONArray userList) {
        Set<String> mailSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(userList)) {
            for (int i = 0; i < userList.size(); i++) {
                String userUuid = userList.getString(i);
                if (("alertUserType#" + AlertUserType.WORKER.getValue()).equals(userUuid)) {
                    //发送给处理人
                    if (CollectionUtils.isEmpty(alertVo.getUserList())) {
                        //告警处理人id为空时尝试查找处理人信息，但如果告警没保存就查不到
                        List<AlertUserVo> alertUserList = alertEventMapper.getAlertUserByAlertId(alertVo.getId());
                        if (CollectionUtils.isNotEmpty(alertUserList)) {
                            for (AlertUserVo user : alertUserList) {
                                if (StringUtils.isNotBlank(user.getUserEmail())) {
                                    mailSet.add(user.getUserEmail());
                                }
                            }
                        }
                    } else {
                        //List<UserVo> foundUserList = userMapper.getUserByUserUuidList(alertVo.getUserIdList());
                        // if (CollectionUtils.isNotEmpty(foundUserList)) {
                        for (AlertUserVo user : alertVo.getUserList()) {
                            if (StringUtils.isNotBlank(user.getUserEmail())) {
                                mailSet.add(user.getUserEmail());
                            }
                        }
                        //}
                    }
                } else if (("alertUserType#" + AlertUserType.WORKER_TEAM.getValue()).equals(userUuid)) {
                    //发送给处理组
                    if (CollectionUtils.isEmpty(alertVo.getTeamList())) {
                        //告警处理人id为空时尝试查找处理组信息，但如果告警没保存就查不到
                        List<AlertTeamVo> alertTeamList = alertEventMapper.getAlertTeamByAlertId(alertVo.getId());
                        if (CollectionUtils.isNotEmpty(alertTeamList)) {
                            for (AlertTeamVo team : alertTeamList) {
                                if (StringUtils.isNotBlank(team.getTeamEmail())) {
                                    mailSet.add(team.getTeamEmail());
                                }
                            }
                        }
                    } else {
                        //List<TeamVo> foundTeamList = teamMapper.getTeamByUuidList(alertVo.getTeamIdList());
                        //if (CollectionUtils.isNotEmpty(foundTeamList)) {
                        for (AlertTeamVo team : alertVo.getTeamList()) {
                            if (StringUtils.isNotBlank(team.getTeamEmail())) {
                                mailSet.add(team.getTeamEmail());
                            }
                        }
                        // }
                    }
                } else if (userUuid.startsWith("user#")) {
                    userUuid = AuthType.removePrefix(userUuid);
                    UserVo userVo = userMapper.getUserByUuid(userUuid);
                    if (userVo != null && StringUtils.isNotBlank(userVo.getEmail())) {
                        mailSet.add(userVo.getEmail());
                    }
                } else if (userUuid.startsWith("team#")) {
                    //如果分组有邮件地址，直接使用
                    userUuid = AuthType.removePrefix(userUuid);
                    TeamVo teamVo = teamMapper.getTeamByUuid(userUuid);
                    if (teamVo != null && StringUtils.isNotBlank(teamVo.getEmail())) {
                        mailSet.add(teamVo.getEmail());
                    }
                }
            }
        }
        return mailSet;
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

    /*@Override
    public List<AlertEventHandlerConfigVo> getHandlerConfig(AlertEventHandlerVo alertEventHandlerVo) {
        List<AlertEventHandlerConfigVo> configList = new ArrayList<>();
        AlertEventHandlerConfigVo alertEventHandlerConfigVo = new AlertEventHandlerConfigVo();
        alertEventHandlerConfigVo.setAlertEventHandlerId(alertEventHandlerVo.getId());
        alertEventHandlerConfigVo.setUuid(alertEventHandlerVo.getUuid());
        alertEventHandlerConfigVo.setHandler(alertEventHandlerVo.getHandler());
        alertEventHandlerConfigVo.setConfig(alertEventHandlerVo.getConfig());
        configList.add(alertEventHandlerConfigVo);
        return configList;
    }*/

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
        }};
    }
}
