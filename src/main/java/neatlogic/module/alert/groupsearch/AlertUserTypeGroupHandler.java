/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
