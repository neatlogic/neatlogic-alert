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

package neatlogic.module.alert.config;

import neatlogic.framework.common.config.IConfigListener;

import java.util.Properties;

public class AlertConfig implements IConfigListener {

    private static int ORIGINAL_ALERT_THREAD_COUNT;

    public static int ORIGINAL_ALERT_THREAD_COUNT() {
        return ORIGINAL_ALERT_THREAD_COUNT;
    }

    @Override
    public void loadConfig(Properties prop) {
        ORIGINAL_ALERT_THREAD_COUNT = Integer.parseInt(prop.getProperty("alert.original.thread.count", "5"));
    }
}
