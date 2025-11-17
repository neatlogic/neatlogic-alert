/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the Sustainable Use License (SUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.alert.attr;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class TeamListFormater implements IAttrFormater {
    @Override
    public String getName() {
        return "const_teamList";
    }

    @Override
    public String getValue(String key, JSONObject alertObj) {
        JSONArray teams = alertObj.getJSONArray("teamList");
        String team = "";
        if (CollectionUtils.isNotEmpty(teams)) {
            for (int i = 0; i < teams.size(); i++) {
                if (StringUtils.isNotBlank(team)) {
                    team += ",";
                }
                team += teams.getJSONObject(i).getString("teamName");
            }
        }
        return team;
    }
}
