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

package neatlogic.module.alert.api.alertcatalog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertCatalogAuthVo;
import neatlogic.framework.alert.dto.AlertCatalogVo;
import neatlogic.framework.alert.exception.alertview.AlertViewIsExistsException;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertCatalogMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class SaveAlertCatalogApi extends PrivateApiComponentBase {

    @Resource
    private AlertCatalogMapper alertCatalogMapper;


    @Override
    public String getToken() {
        return "alert/catalog/save";
    }

    @Override
    public String getName() {
        return "保存告警目录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "name", desc = "名称", isRequired = true, maxLength = 50, type = ApiParamType.STRING),
            @Param(name = "isActive", desc = "是否激活", type = ApiParamType.INTEGER),
            @Param(name = "authList", desc = "授权列表", type = ApiParamType.JSONARRAY)
    })
    @Output({@Param(name = "id", desc = "视图id", type = ApiParamType.LONG)})
    @Description(desc = "保存告警目录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        AlertCatalogVo alertCatalogVo = JSON.toJavaObject(jsonObj, AlertCatalogVo.class);
        if (alertCatalogMapper.checkAlertCatalogIsExists(alertCatalogVo) > 0) {
            throw new AlertViewIsExistsException(alertCatalogVo.getName());
        }
        Long id = jsonObj.getLong("id");
        if (id != null) {
            alertCatalogVo.setLcu(UserContext.get().getUserUuid(true));
            alertCatalogMapper.deleteAlertCatalogAuthByCatalogId(id);
        } else {
            alertCatalogVo.setFcu(UserContext.get().getUserUuid(true));
        }
        //清除权限，重新从前端数据中获取
        alertCatalogVo.setAlertCatalogAuthList(null);
        alertCatalogMapper.saveAlertCatalog(alertCatalogVo);
        if (CollectionUtils.isNotEmpty(alertCatalogVo.getAlertCatalogAuthList())) {
            for (AlertCatalogAuthVo authVo : alertCatalogVo.getAlertCatalogAuthList()) {
                authVo.setCatalogId(alertCatalogVo.getId());
                alertCatalogMapper.insertAlertCatalogAuth(authVo);
            }
        }
        return alertCatalogVo.getId();
    }


}
