package neatlogic.module.alert.breaker.action;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerActionVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerPolicyVo;
import neatlogic.framework.alert.dto.breaker.AlertBreakerStateVo;
import neatlogic.framework.alert.enums.AlertBreakerActionTrigger;
import neatlogic.framework.alert.utils.AlertEventHandlerContextBuilder;
import neatlogic.framework.util.EmailUtil;
import neatlogic.framework.util.FreemarkerUtil;
import neatlogic.module.alert.dto.AlertMailReceiverVo;
import neatlogic.module.alert.service.AlertMailReceiverService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
public class SendMailAlertBreakerActionHandler extends AlertBreakerActionTemplateBase {
    @Resource
    private AlertMailReceiverService alertMailReceiverService;
    @Resource
    private AlertEventHandlerContextBuilder alertEventHandlerContextBuilder;

    @Override
    public String getName() {
        return "sendmail";
    }

    @Override
    public String getLabel() {
        return "发送邮件";
    }

    @Override
    public String getDescription() {
        return "在熔断生命周期中发送邮件，可用于熔断通知、聚合通知和恢复通知。";
    }

    @Override
    public Set<AlertBreakerActionTrigger> supportTrigger() {
        return EnumSet.allOf(AlertBreakerActionTrigger.class);
    }

    @Override
    protected void myTriggerOpen(AlertBreakerActionVo actionVo, AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, AlertVo alertVo) throws Exception {
        sendMail(actionVo, AlertBreakerActionTrigger.OPEN, alertVo, toAlertList(alertVo));
    }

    @Override
    protected void myTriggerAggregate(AlertBreakerActionVo actionVo, AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, List<AlertVo> alertList) throws Exception {
        sendMail(actionVo, AlertBreakerActionTrigger.AGGREGATE, getLastAlert(alertList), alertList);
    }

    @Override
    protected void myTriggerRecover(AlertBreakerActionVo actionVo, AlertBreakerPolicyVo policyVo, AlertBreakerStateVo stateVo, List<AlertVo> alertList) throws Exception {
        sendMail(actionVo, AlertBreakerActionTrigger.RECOVER, getLastAlert(alertList), alertList);
    }

    private void sendMail(AlertBreakerActionVo actionVo, AlertBreakerActionTrigger trigger, AlertVo alertVo, List<AlertVo> alertList) throws Exception {
        JSONObject config = actionVo.getConfig();
        List<AlertVo> fullAlertList = buildFullAlertList(alertList);
        AlertVo fullAlertVo = alertEventHandlerContextBuilder.build(alertVo);
        if (fullAlertVo == null) {
            fullAlertVo = getLastAlert(fullAlertList);
        }
        AlertMailReceiverVo receiverVo = alertMailReceiverService.getReceiver(fullAlertVo, config.getJSONArray("toUserList"), config.getJSONArray("ccUserList"));
        if (receiverVo == null || (CollectionUtils.isEmpty(receiverVo.getToList()) && CollectionUtils.isEmpty(receiverVo.getCcList()))) {
            throw new IllegalArgumentException("缺少邮件收件人");
        }
        JSONObject paramObj = trigger == AlertBreakerActionTrigger.AGGREGATE ? buildAggregateTemplateParamObj(fullAlertList) : buildSingleAlertTemplateParamObj(fullAlertVo);
        String title = FreemarkerUtil.transform(paramObj, StringUtils.defaultIfBlank(config.getString("title"), getDefaultTitle(trigger)));
        String content = config.getString("content");
        content = StringUtils.isBlank(content) ? getDefaultContent(trigger, fullAlertList) : FreemarkerUtil.transform(paramObj, content);
        EmailUtil.sendHtmlEmail(config.getLong("mailServerId"), title, content, new ArrayList<>(receiverVo.getToList()), new ArrayList<>(receiverVo.getCcList()));
    }

    private List<AlertVo> buildFullAlertList(List<AlertVo> alertList) {
        List<AlertVo> fullAlertList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(alertList)) {
            for (AlertVo alertVo : alertList) {
                AlertVo fullAlertVo = alertEventHandlerContextBuilder.build(alertVo);
                if (fullAlertVo != null) {
                    fullAlertList.add(fullAlertVo);
                }
            }
        }
        return fullAlertList;
    }

    private String getDefaultTitle(AlertBreakerActionTrigger trigger) {
        if (trigger == AlertBreakerActionTrigger.OPEN) {
            return "[告警中心][熔断通知]熔断策略已触发";
        } else if (trigger == AlertBreakerActionTrigger.RECOVER) {
            return "[告警中心][熔断恢复]熔断策略已恢复";
        }
        return "[告警中心][聚合通知]共${DATA.alertCount}条告警待处理";
    }

    private String getDefaultContent(AlertBreakerActionTrigger trigger, List<AlertVo> alertList) {
        return trigger == AlertBreakerActionTrigger.AGGREGATE ? buildDefaultAggregateContent(alertList) : "";
    }
}
