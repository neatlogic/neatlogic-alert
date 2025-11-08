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

package neatlogic.module.alert.mq;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.exception.mq.SubscribeConfigNotFoundException;
import neatlogic.framework.mq.core.SubscribeHandlerBase;
import neatlogic.framework.mq.dto.SubscribeVo;
import neatlogic.module.alert.queue.OriginalAlertManager;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AlertSubscribe extends SubscribeHandlerBase {

    @Override
    protected void myOnMessage(SubscribeVo subscribeVo, Object message) {
        if (message != null) {
            JSONObject config = subscribeVo.getConfig();
            if (MapUtils.isNotEmpty(config) && config.containsKey("alertType") && config.containsKey("adaptor")) {
                String alertType = config.getString("alertType");
                String adaptor = config.getString("adaptor");
                OriginalAlertVo alertVo = new OriginalAlertVo();
                alertVo.setSource(InputFrom.MQ.getValue());
                alertVo.setContent(message.toString());
                alertVo.setTime(new Date());
                alertVo.setType(alertType);
                alertVo.setAdaptor(adaptor);
                OriginalAlertManager.addAlert(alertVo);
            } else {
                throw new SubscribeConfigNotFoundException(subscribeVo.getName(), "alertType或adaptor");
            }
        }
    }

    @Override
    public String getName() {
        return "ALERT_SUBSCRIBE";
    }

    @Override
    public String getLabel() {
        return "告警处理组件";
    }
}
