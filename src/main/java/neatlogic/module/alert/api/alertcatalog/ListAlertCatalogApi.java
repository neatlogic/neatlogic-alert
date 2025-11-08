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
import com.alibaba.nacos.common.utils.CollectionUtils;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertCatalogVo;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertCatalogMapper;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertCatalogApi extends PrivateApiComponentBase {

    @Resource
    private AlertCatalogMapper alertCatalogMapper;

    @Resource
    private AlertViewMapper alertViewMapper;

    @Override
    public String getToken() {
        return "/alert/catalog/list";
    }

    @Override
    public String getName() {
        return "返回所有告警目录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Output({@Param(explode = AlertCatalogVo[].class)})
    @Description(desc = "返回所有告警目录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertCatalogVo alertCatalogVo = JSON.toJavaObject(jsonObj, AlertCatalogVo.class);
        alertCatalogVo.setAdmin(true);
        List<AlertCatalogVo> catalogList = alertCatalogMapper.listAlertCatalog(alertCatalogVo);
        if (CollectionUtils.isNotEmpty(catalogList)) {
            for (AlertCatalogVo catalogVo : catalogList) {
                AlertViewVo alertViewVo = new AlertViewVo();
                alertViewVo.setCatalogId(catalogVo.getId());
                alertViewVo.setAdmin(true);
                catalogVo.setViewList(alertViewMapper.listAlertView(alertViewVo));
            }
        }
        return catalogList;
    }

}
