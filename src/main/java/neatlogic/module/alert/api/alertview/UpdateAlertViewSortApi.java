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

package neatlogic.module.alert.api.alertview;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class UpdateAlertViewSortApi extends PrivateApiComponentBase {

    @Resource
    private AlertViewMapper alertViewMapper;


    @Override
    public String getToken() {
        return "alert/view/sort/update";
    }

    @Override
    public String getName() {
        return "更新告警视图排序";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "catalogId", desc = "目录id", type = ApiParamType.LONG),
            @Param(name = "idList", desc = "id", type = ApiParamType.JSONARRAY, isRequired = true)
    })
    @Description(desc = "更新告警视图排序")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        Long catalogId = jsonObj.getLong("catalogId");
        if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                Long id = idList.getLong(i);
                AlertViewVo viewVo = new AlertViewVo();
                viewVo.setId(id);
                viewVo.setCatalogId(catalogId);
                viewVo.setSort(i + 1);
                alertViewMapper.updateAlertViewSort(viewVo);
            }
        }
        return null;
    }


}
