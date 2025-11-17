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

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AttrFormaterFactory {
    private static final Map<String, IAttrFormater> attrFormaterMap = new HashMap<>();

    static {
        Reflections reflections = new Reflections("neatlogic");
        Set<Class<? extends IAttrFormater>> modules = reflections.getSubTypesOf(IAttrFormater.class);
        for (Class<? extends IAttrFormater> c : modules) {
            IAttrFormater handler;
            try {
                handler = c.newInstance();
                if (StringUtils.isNotBlank(handler.getName())) {
                    attrFormaterMap.put(handler.getName(), handler);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static IAttrFormater getFormater(String name) {
        if (attrFormaterMap.containsKey(name)) {
            return attrFormaterMap.get(name);
        } else {
            return attrFormaterMap.get("*");
        }
    }
}
