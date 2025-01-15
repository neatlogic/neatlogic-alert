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

import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.common.constvalue.InputFrom;
import neatlogic.framework.mq.core.SubscribeHandlerBase;
import neatlogic.framework.mq.dto.SubscribeVo;
import neatlogic.module.alert.queue.OriginalAlertManager;
import org.springframework.stereotype.Component;

@Component
public class AlertSubscribe extends SubscribeHandlerBase {
    @Override
    protected void myOnMessage(SubscribeVo subscribeVo, Object message) {
        if (message != null) {
            OriginalAlertVo alertVo = new OriginalAlertVo();
            alertVo.setSource(InputFrom.MQ.getValue());
            alertVo.setContent(message.toString());
            alertVo.setType("test");
            OriginalAlertManager.addAlert(alertVo);
        }
    }

    @Override
    public String getName() {
        return "告警处理组件";
    }
}
