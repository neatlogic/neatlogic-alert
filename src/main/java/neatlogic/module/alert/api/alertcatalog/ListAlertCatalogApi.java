/*
 * Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.
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
