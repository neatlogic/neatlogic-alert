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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_VIEW_MODIFY;
import neatlogic.framework.alert.dto.AlertCatalogVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamIrregularException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertCatalogMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class UpdateAlertCatalogSortApi extends PrivateApiComponentBase {

    @Resource
    private AlertCatalogMapper alertCatalogMapper;


    @Override
    public String getToken() {
        return "alert/catalog/sort/update";
    }

    @Override
    public String getName() {
        return "更新告警目录排序";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "parentId", desc = "父目录id", type = ApiParamType.LONG),
            @Param(name = "idList", desc = "id", type = ApiParamType.JSONARRAY, isRequired = true)
    })
    @Description(desc = "更新告警目录排序")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        Long parentId = jsonObj.getLong("parentId");
        if (CollectionUtils.isNotEmpty(idList)) {
            validateParent(parentId, idList);
            for (int i = 0; i < idList.size(); i++) {
                Long id = idList.getLong(i);
                AlertCatalogVo catalogVo = new AlertCatalogVo();
                catalogVo.setId(id);
                catalogVo.setParentId(parentId);
                catalogVo.setSort(i + 1);
                alertCatalogMapper.updateAlertCatalogSort(catalogVo);
            }
        }
        return null;
    }

    private void validateParent(Long parentId, JSONArray idList) {
        if (parentId == null || CollectionUtils.isEmpty(idList)) {
            return;
        }
        AlertCatalogVo queryVo = new AlertCatalogVo();
        queryVo.setAdmin(true);
        List<AlertCatalogVo> catalogList = alertCatalogMapper.listAlertCatalog(queryVo);
        Map<Long, AlertCatalogVo> catalogMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(catalogList)) {
            for (AlertCatalogVo catalogVo : catalogList) {
                catalogMap.put(catalogVo.getId(), catalogVo);
            }
        }
        for (int i = 0; i < idList.size(); i++) {
            Long id = idList.getLong(i);
            if (parentId.equals(id)) {
                throw new ParamIrregularException("parentId");
            }
            Long currentParentId = parentId;
            while (currentParentId != null) {
                if (currentParentId.equals(id)) {
                    throw new ParamIrregularException("parentId");
                }
                AlertCatalogVo parentCatalog = catalogMap.get(currentParentId);
                if (parentCatalog == null) {
                    break;
                }
                currentParentId = parentCatalog.getParentId();
            }
        }
    }


}
