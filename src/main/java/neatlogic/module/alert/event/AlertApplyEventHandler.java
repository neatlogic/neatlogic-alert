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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Component
public class AlertApplyEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertMapper alertMapper;


    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo) throws AlertEventHandlerTriggerException {
        JSONObject config = alertEventHandlerVo.getConfig();
        if (MapUtils.isNotEmpty(config)) {
            JSONArray userIdList = config.getJSONArray("userIdList");
            JSONArray teamIdList = config.getJSONArray("teamIdList");
            IElasticsearchIndex<AlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT");
            if (CollectionUtils.isNotEmpty(userIdList)) {
                for (int i = 0; i < userIdList.size(); i++) {
                    String userId = userIdList.getString(i);
                    AlertUserVo alertUserVo = new AlertUserVo();
                    alertUserVo.setAlertId(alertVo.getId());
                    alertUserVo.setUserId(userId);
                    alertMapper.insertAlertUser(alertUserVo);
                }
            }
            if (CollectionUtils.isNotEmpty(teamIdList)) {
                for (int i = 0; i < teamIdList.size(); i++) {
                    String teamId = teamIdList.getString(i);
                    AlertTeamVo alertTeamVo = new AlertTeamVo();
                    alertTeamVo.setAlertId(alertVo.getId());
                    alertTeamVo.setTeamUuid(teamId);
                    alertMapper.insertAlertTeam(alertTeamVo);
                }
            }
            indexHandler.updateDocument(alertVo.getId(), new JSONObject() {{
                this.put("userList", userIdList);
                this.put("teamList", teamIdList);
            }});
        }
        return alertVo;
    }

    @Override
    public String getName() {
        return "APPLY";
    }

    @Override
    public String getLabel() {
        return "分配处理人";
    }

    @Override
    public String getIcon() {
        return "tsfont-team-s";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_SAVE.getName());
        }};
    }
}
