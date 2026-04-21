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

package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.dto.AlertNotifyTemplateVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.alert.exception.alertnotifytemplate.NotifyTemplateIsUnActiveException;
import neatlogic.framework.alert.exception.alertnotifytemplate.NotifyTemplateNameIsNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.constvalue.AuthType;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.TeamVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.EmailUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertNotifyTemplateMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SendAlertMailApi extends PrivateApiComponentBase {

    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertNotifyTemplateMapper alertNotifyTemplateMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;

    @Override
    public String getToken() {
        return "alert/mail/send";
    }

    @Override
    public String getName() {
        return "发送告警邮件";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "alertId", desc = "告警id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "toUserList", desc = "收件人列表", isRequired = true, type = ApiParamType.JSONARRAY),
            @Param(name = "templateId", desc = "通知模板id", type = ApiParamType.LONG),
            @Param(name = "title", desc = "标题", type = ApiParamType.STRING),
            @Param(name = "content", desc = "内容", type = ApiParamType.STRING)
    })
    @Description(desc = "发送告警邮件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long alertId = jsonObj.getLong("alertId");
        AlertVo alertVo = alertMapper.getAlertById(alertId);
        if (alertVo == null) {
            throw new AlertNotFoundException(alertId);
        }

        String title;
        String content;
        Long templateId = jsonObj.getLong("templateId");
        if (templateId != null) {
            AlertNotifyTemplateVo templateVo = alertNotifyTemplateMapper.getNotifyTemplateById(templateId);
            if (templateVo == null) {
                throw new NotifyTemplateNameIsNotFoundException(templateId);
            }
            if (Objects.equals(templateVo.getIsActive(), 0)) {
                throw new NotifyTemplateIsUnActiveException(templateVo.getName());
            }
            title = templateVo.getTitle();
            content = templateVo.getContent();
        } else {
            title = jsonObj.getString("title");
            content = jsonObj.getString("content");
            if (StringUtils.isBlank(title)) {
                throw new ParamNotExistsException("title");
            }
            if (StringUtils.isBlank(content)) {
                throw new ParamNotExistsException("content");
            }
        }

        JSONObject freemarkerParam = getFreemarkerParam(alertVo);
        title = FreemarkerUtil.transform(freemarkerParam, title);
        content = FreemarkerUtil.transform(freemarkerParam, content);

        Set<String> to = makeupMailList(jsonObj.getJSONArray("toUserList"));
        if (CollectionUtils.isEmpty(to)) {
            throw new ApiRuntimeException("收件人邮箱地址为空");
        }
        EmailUtil.sendHtmlEmail(title, content, new ArrayList<>(to), null);
        return null;
    }

    private JSONObject getFreemarkerParam(AlertVo alertVo) {
        JSONObject paramObj = new JSONObject();
        JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
        List<AlertAttrDefineVo> attrList = AlertAttr.getTemplateConstAttrList();
        for (AlertAttrDefineVo attr : attrList) {
            paramObj.put(attr.getName(), alertObj.get(attr.getName().replace("const_", "")));
        }
        if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
            List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
            for (AlertAttrTypeVo alertAttr : attrTypeList) {
                paramObj.put("attr_" + alertAttr.getName(), alertVo.getAttrObj().get(alertAttr.getName()));
            }
        }
        return paramObj;
    }

    private Set<String> makeupMailList(JSONArray userList) {
        Set<String> mailSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(userList)) {
            for (int i = 0; i < userList.size(); i++) {
                String userUuid = userList.getString(i);
                if (StringUtils.isBlank(userUuid)) {
                    continue;
                }
                if (userUuid.startsWith("user#")) {
                    userUuid = AuthType.removePrefix(userUuid);
                    UserVo userVo = userMapper.getUserByUuid(userUuid);
                    if (userVo != null && StringUtils.isNotBlank(userVo.getEmail())) {
                        mailSet.add(userVo.getEmail());
                    }
                } else if (userUuid.startsWith("team#")) {
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
}
