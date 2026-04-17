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

package neatlogic.module.alert.attr.freemarker;

import neatlogic.framework.alert.attr.freemarker.IAlertAttrFreemarkerSnippetHandler;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.common.config.Config;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CsvAlertAttrFreemarkerSnippetHandler implements IAlertAttrFreemarkerSnippetHandler {

    @Override
    public String getName() {
        return "csv";
    }

    @Override
    public String getFreemarkerSnippet(AlertAttrTypeVo alertAttrTypeVo) {
        String backEndUrl = Config.HOME_URL();
        if (StringUtils.isNotBlank(backEndUrl) && !backEndUrl.endsWith("/")) {
            backEndUrl += "/";
        }
        backEndUrl += TenantContext.get().getTenantUuid();
        String attrName = encode(alertAttrTypeVo.getName());
        String url = StringUtils.defaultString(backEndUrl) + "/any/api/binary/alert/attr/csv/download?alertId=${DATA.const_id}&attrName=" + attrName;
        return "<a href=\"" + url + "\" target=\"_blank\">data.csv</a>";
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }
}
