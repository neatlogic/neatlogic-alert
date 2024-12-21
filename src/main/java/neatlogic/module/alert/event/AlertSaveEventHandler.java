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
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertEventHandlerVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertStatus;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.util.Md5Util;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AlertSaveEventHandler extends AlertEventHandlerBase {
    @Resource
    private IAlertService alertService;

    @Resource
    private AlertMapper alertMapper;

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertVo alertVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        //根据唯一规则计算uniquekey
        if (config != null && config.getJSONArray("uniqueAttrList") != null) {
            List<String> attrList = new ArrayList<>();
            for (int i = 0; i < config.getJSONArray("uniqueAttrList").size(); i++) {
                attrList.add(config.getJSONArray("uniqueAttrList").getJSONObject(i).getString("name"));
            }
            //按属性名排序，避免由于顺序不同导致结果不同
            attrList.sort(String::compareTo);
            String key = "";
            JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
            for (String attr : attrList) {
                if (attr.startsWith("const_")) {
                    if (StringUtils.isNotBlank(key)) {
                        key += "#";
                    }
                    key += alertObj.getString(attr.substring("const_".length()));
                } else if (attr.startsWith("attr_")) {
                    JSONObject attrObj = alertObj.getJSONObject("attrObj");
                    if (attrObj != null && attrObj.get(attr.substring("attr_".length())) != null) {
                        if (StringUtils.isNotBlank(key)) {
                            key += "#";
                        }
                        key += attrObj.getString(attr.substring("attr_".length()));
                    }
                }
            }
            if (StringUtils.isNotBlank(key)) {
                alertVo.setUniqueKey(Md5Util.encryptMD5(key));
            }
        }
        if (StringUtils.isBlank(alertVo.getUniqueKey())) {
            alertVo.generateUniqueKey();
        }
        alertVo.setStatus(AlertStatus.NEW.getValue());
        alertService.saveAlert(alertVo);
        return alertVo;
    }

    @Override
    public String getName() {
        return "SAVE";
    }

    @Override
    public String getLabel() {
        return "保存告警";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            this.add(AlertEventType.ALERT_INPUT.getName());
        }};
    }

}
