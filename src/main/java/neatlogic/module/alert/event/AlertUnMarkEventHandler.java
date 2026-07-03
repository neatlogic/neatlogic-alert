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

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import neatlogic.framework.util.$;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMarkMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AlertUnMarkEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertAuditMapper alertAuditMapper;

    @Resource
    private AlertMarkMapper alertMarkMapper;

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public int getSort() {
        return 14;
    }

    @Override
    public String getName() {
        return "UNMARK";
    }

    @Override
    public String getLabel() {
        return $.t("term.alert.event.unmarkhandlername");
    }

    @Override
    public String getIcon() {
        return "tsfont-non-auth";
    }

    @Override
    public String getDescription() {
        return $.t("term.alert.event.unmarkhandlerdesc");
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
            this.add(AlertEventType.ALERT_SUPPRESS.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<>() {{
            this.add("condition");
            this.add("interval");
            this.add("integration");
        }};
    }


    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (config == null) {
            config = new JSONObject();
        }

        JSONArray markList = config.getJSONArray("markList");
        if (CollectionUtils.isNotEmpty(markList)) {
            List<AlertMarkVo> oldMarkList = alertMarkMapper.getAlertMarkByAlertId(alertVo.getId());
            JSONArray oldMarkObjList = new JSONArray();
            if (CollectionUtils.isNotEmpty(oldMarkList)) {
                for (AlertMarkVo oldMarkVo : oldMarkList) {
                    oldMarkObjList.add(oldMarkVo.getName());
                }
            }

            for (int i = 0; i < markList.size(); i++) {
                String name = markList.getString(i);
                alertMarkMapper.deleteAlertAlertMark(alertVo.getId(), Md5Util.encryptMD5(name));
            }

            List<AlertMarkVo> newMarkList = alertMarkMapper.getAlertMarkByAlertId(alertVo.getId());
            JSONArray newMarkObjList = new JSONArray();
            if (CollectionUtils.isNotEmpty(newMarkList)) {
                for (AlertMarkVo newMarkVo : newMarkList) {
                    newMarkObjList.add(newMarkVo.getName());
                }
            }

            AlertAuditVo alertAuditVo = new AlertAuditVo(true);
            alertAuditVo.setAlertId(alertVo.getId());
            alertAuditVo.setAttrName("const_markList");
            alertAuditVo.setOldValueList(oldMarkObjList);
            alertAuditVo.setNewValueList(newMarkObjList);
            alertAuditMapper.insertAlertAudit(alertAuditVo);

            IElasticsearchDocument<AlertVo> index = ElasticsearchDocumentFactory.getIndex("ALERT");
            index.updateDocument(alertVo.getId(), new JSONObject() {{
                this.put("markList", newMarkObjList);
            }}, false);
        }
        return alertVo;
    }
}
