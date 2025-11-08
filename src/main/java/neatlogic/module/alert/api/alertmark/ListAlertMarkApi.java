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

package neatlogic.module.alert.api.alertmark;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertMarkVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertMarkMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertMarkApi extends PrivateApiComponentBase {


    @Resource
    private AlertMarkMapper alertMarkMapper;

    @Override
    public String getToken() {
        return "/alert/mark/list";
    }

    @Override
    public String getName() {
        return "列出符合条件的标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "nameList", desc = "名称列表", type = ApiParamType.JSONARRAY)
    })
    @Description(desc = "列出符合条件的标签")
    @Output({@Param(explode = AlertMarkVo[].class)})
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        JSONArray jsonArray = jsonObj.getJSONArray("nameList");
        String keyword = jsonObj.getString("keyword");
        List<String> nameList = new ArrayList<>();
        if (StringUtils.isNotEmpty(keyword)) {
            nameList.add(keyword);
        }
        if (CollectionUtils.isNotEmpty(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                nameList.add(jsonArray.getString(i));
            }
        }
        return alertMarkMapper.listAlertMarkByNameList(nameList);
    }
}
