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

package neatlogic.module.alert.groupsearch;

import neatlogic.framework.alert.enums.AlertUserType;
import neatlogic.framework.restful.groupsearch.core.GroupSearchOptionVo;
import neatlogic.framework.restful.groupsearch.core.GroupSearchVo;
import neatlogic.framework.restful.groupsearch.core.IGroupSearchHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertUserTypeGroupHandler implements IGroupSearchHandler {
    @Override
    public String getName() {
        return "alertUserType";
    }

    @Override
    public String getLabel() {
        return "告警处理人";
    }


    @Override
    public List<GroupSearchOptionVo> search(GroupSearchVo groupSearchVo) {
        List<GroupSearchOptionVo> groupSearchOptionList = new ArrayList<>();
        groupSearchOptionList.add(new GroupSearchOptionVo() {{
            this.setValue(getHeader() + "worker");
            this.setText("处理人");
        }});
        groupSearchOptionList.add(new GroupSearchOptionVo() {{
            this.setValue(getHeader() + "workerteam");
            this.setText("处理组");
        }});
        groupSearchOptionList.add(new GroupSearchOptionVo() {{
            this.setValue(getHeader() + "workerteamuser");
            this.setText("处理组成员");
        }});
        return groupSearchOptionList;
    }

    @Override
    public List<GroupSearchOptionVo> reload(GroupSearchVo groupSearchVo) {
        List<GroupSearchOptionVo> groupSearchOptionList = new ArrayList<>();
        List<String> valueList = groupSearchVo.getValueList();
        if (CollectionUtils.isNotEmpty(valueList)) {
            for (String value : valueList) {
                if (value.startsWith(getHeader())) {
                    value = value.substring(getHeader().length());
                    AlertUserType alertUserType = AlertUserType.get(value);
                    if (alertUserType != null) {
                        GroupSearchOptionVo groupSearchOptionVo = new GroupSearchOptionVo();
                        groupSearchOptionVo.setValue(getHeader() + alertUserType.getValue());
                        groupSearchOptionVo.setText(alertUserType.getText());
                        groupSearchOptionList.add(groupSearchOptionVo);
                    }
                }
            }

        }
        return groupSearchOptionList;
    }


    @Override
    public int getSort() {
        return 0;
    }

    @Override
    public Boolean isLimit() {
        return false;
    }
}
