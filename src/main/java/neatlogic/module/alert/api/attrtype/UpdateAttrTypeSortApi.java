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

package neatlogic.module.alert.api.attrtype;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ATTR_MODIFY;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_ATTR_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class UpdateAttrTypeSortApi extends PrivateApiComponentBase {

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Override
    public String getToken() {
        return "/alert/attrtype/sort/update";
    }

    @Override
    public String getName() {
        return "更新扩展属性排序";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", desc = "id列表", type = ApiParamType.JSONARRAY, isRequired = true)})
    @Description(desc = "更新扩展属性排序")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        JSONArray idList = jsonObj.getJSONArray("idList");
        for (int i = 0; i < idList.size(); i++) {
            AlertAttrTypeVo alertAttrTypeVo = new AlertAttrTypeVo();
            alertAttrTypeVo.setSort(i + 1);
            alertAttrTypeVo.setId(idList.getLong(i));
            alertAttrTypeMapper.updateAlertAttrTypeSort(alertAttrTypeVo);
        }
        return null;
    }

}
