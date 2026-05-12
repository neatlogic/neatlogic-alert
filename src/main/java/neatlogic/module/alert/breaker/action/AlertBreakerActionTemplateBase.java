package neatlogic.module.alert.breaker.action;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.breaker.action.AlertBreakerActionHandlerBase;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.common.config.Config;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

public abstract class AlertBreakerActionTemplateBase extends AlertBreakerActionHandlerBase {
    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    protected JSONObject buildSingleAlertTemplateParamObj(AlertVo alertVo) {
        JSONObject paramObj = new JSONObject();
        if (alertVo != null) {
            JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
            for (AlertAttrDefineVo attr : AlertAttr.getTemplateConstAttrList()) {
                paramObj.put(attr.getName(), alertObj.get(attr.getName().replace("const_", "")));
            }
            if (MapUtils.isNotEmpty(alertVo.getAttrObj())) {
                List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
                for (AlertAttrTypeVo alertAttr : attrTypeList) {
                    paramObj.put("attr_" + alertAttr.getName(), alertVo.getAttrObj().get(alertAttr.getName()));
                }
            }
        }
        return paramObj;
    }

    protected JSONObject buildAggregateTemplateParamObj(List<AlertVo> alertList) {
        JSONObject paramObj = buildSingleAlertTemplateParamObj(getLastAlert(alertList));
        JSONArray alertItemList = buildAggregateItemList(alertList);
        paramObj.put("alertCount", alertList == null ? 0 : alertList.size());
        paramObj.put("alertItemList", alertItemList);
        paramObj.put("alertList", buildDefaultAggregateContent(alertList));
        return paramObj;
    }

    protected List<AlertVo> toAlertList(AlertVo alertVo) {
        return alertVo == null ? Collections.emptyList() : Collections.singletonList(alertVo);
    }

    protected AlertVo getLastAlert(List<AlertVo> alertList) {
        if (alertList == null || alertList.isEmpty()) {
            return null;
        }
        return alertList.get(alertList.size() - 1);
    }

    protected JSONArray buildAggregateItemList(List<AlertVo> alertList) {
        JSONArray itemList = new JSONArray();
        if (alertList == null) {
            return itemList;
        }
        for (AlertVo alertVo : alertList) {
            JSONObject item = new JSONObject();
            item.put("id", alertVo.getId());
            item.put("title", alertVo.getTitle());
            item.put("level", StringUtils.defaultIfBlank(alertVo.getLevelLabel(), alertVo.getLevel() == null ? null : String.valueOf(alertVo.getLevel())));
            item.put("source", StringUtils.defaultIfBlank(alertVo.getSourceName(), alertVo.getSource()));
            item.put("status", StringUtils.defaultIfBlank(alertVo.getStatusName(), alertVo.getStatus()));
            item.put("alertTime", alertVo.getAlertTimeStr());
            item.put("detailUrl", buildAlertUrl(alertVo.getId()));
            itemList.add(item);
        }
        return itemList;
    }

    protected String buildDefaultAggregateContent(List<AlertVo> alertList) {
        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;margin-top:12px;width:100%;\">");
        html.append("<thead><tr><th>ID</th><th>标题</th><th>级别</th><th>来源</th><th>状态</th><th>告警时间</th><th>详情</th></tr></thead><tbody>");
        if (alertList != null) {
            for (AlertVo alertVo : alertList) {
                html.append("<tr>");
                html.append("<td>").append(display(alertVo.getId())).append("</td>");
                html.append("<td>").append(display(alertVo.getTitle())).append("</td>");
                html.append("<td>").append(display(StringUtils.defaultIfBlank(alertVo.getLevelLabel(), alertVo.getLevel() == null ? null : String.valueOf(alertVo.getLevel())))).append("</td>");
                html.append("<td>").append(display(StringUtils.defaultIfBlank(alertVo.getSourceName(), alertVo.getSource()))).append("</td>");
                html.append("<td>").append(display(StringUtils.defaultIfBlank(alertVo.getStatusName(), alertVo.getStatus()))).append("</td>");
                html.append("<td>").append(display(alertVo.getAlertTimeStr())).append("</td>");
                html.append("<td>").append(buildLink(buildAlertUrl(alertVo.getId()))).append("</td>");
                html.append("</tr>");
            }
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildAlertUrl(Long alertId) {
        String homeUrl = Config.HOME_URL();
        if (alertId == null || StringUtils.isBlank(homeUrl) || TenantContext.get() == null) {
            return null;
        }
        if (!homeUrl.endsWith("/")) {
            homeUrl += "/";
        }
        return homeUrl + TenantContext.get().getTenantUuid() + "/alert.html#/alert-detail/" + alertId;
    }

    private String buildLink(String url) {
        if (StringUtils.isBlank(url)) {
            return "-";
        }
        return "<a href=\"" + escape(url) + "\">查看</a>";
    }

    private String display(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return "-";
        }
        return escape(String.valueOf(value));
    }

    private String escape(String value) {
        if (value == null) {
            return "-";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
