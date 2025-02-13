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

package neatlogic.module.alert.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.mq.core.TopicBase;
import neatlogic.framework.mq.dto.TopicVo;
import org.springframework.stereotype.Component;

@Component
public class AlertKafkaTopic extends TopicBase<AlertVo> {
    @Override
    protected JSONObject generateTopicContent(TopicVo topicVo, AlertVo content) {
        if (content != null) {
            return JSON.parseObject(JSON.toJSONString(content));
        }
        return null;
    }

    @Override
    public String getName() {
        return "neatlogic_alert";
    }

    @Override
    public String getLabel() {
        return "告警上报";
    }

    @Override
    public String getDescription() {
        return "告警上报主题";
    }

    @Override
    public String getHandler() {
        return "kafka";
    }

    @Override
    public Boolean hasConfig() {
        return false;
    }
}
