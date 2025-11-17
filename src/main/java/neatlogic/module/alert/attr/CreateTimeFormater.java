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

import com.alibaba.fastjson.JSONObject;

import java.text.SimpleDateFormat;

public class CreateTimeFormater implements IAttrFormater {
    @Override
    public String getName() {
        return "const_alertTime";
    }

    @Override
    public String getValue(String key, JSONObject alertObj) {
        Long alertTime = alertObj.getLong("alertTime");
        if (alertTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(alertTime);
        }
        return "";
    }
}
