/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
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

package neatlogic.module.alert.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.AlertRuleVo;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class AlertRuleUtils {
    public static String doRule(String content, List<AlertRuleVo> alertRuleList) {
        for (AlertRuleVo alertRuleVo : alertRuleList) {
            JSONArray ruleList = alertRuleVo.getConfig().getJSONArray("ruleList");
            if (CollectionUtils.isNotEmpty(ruleList)) {
                for (int i = 0; i < ruleList.size(); i++) {
                    JSONObject rule = ruleList.getJSONObject(i);
                    String pattern = rule.getString("pattern");
                    String replacement = rule.getString("replacement");
                    content = content.replaceAll(pattern, replacement);
                }
            }
        }
        return content;
    }
}
