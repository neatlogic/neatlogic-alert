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
import neatlogic.framework.alert.dto.AlertCatalogVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertCatalogMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = ALERT_VIEW_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertCatalogTreeApi extends PrivateApiComponentBase {

    @Resource
    private AlertCatalogMapper alertCatalogMapper;

    @Override
    public String getToken() {
        return "/alert/catalog/listtree";
    }

    @Override
    public String getName() {
        return "返回告警目录树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字")
    })
    @Output({
            @Param(explode = AlertCatalogVo[].class, desc = "目录树")
    })
    @Description(desc = "返回告警目录树")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertCatalogVo queryVo = JSON.toJavaObject(jsonObj, AlertCatalogVo.class);
        queryVo.setAdmin(true);
        List<AlertCatalogVo> catalogList = alertCatalogMapper.listAlertCatalog(queryVo);
        if (CollectionUtils.isEmpty(catalogList)) {
            return Collections.emptyList();
        }
        String keyword = jsonObj.getString("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            List<AlertCatalogVo> filterList = new ArrayList<>();
            for (AlertCatalogVo catalogVo : catalogList) {
                if (StringUtils.containsIgnoreCase(catalogVo.getName(), keyword)) {
                    filterList.add(catalogVo);
                }
            }
            catalogList = filterList;
        }
        return buildTree(catalogList);
    }

    private List<AlertCatalogVo> buildTree(List<AlertCatalogVo> catalogList) {
        Map<Long, AlertCatalogVo> catalogMap = new LinkedHashMap<>();
        for (AlertCatalogVo catalogVo : catalogList) {
            catalogVo.setChildren(new ArrayList<>());
            catalogMap.put(catalogVo.getId(), catalogVo);
        }
        Set<Long> childIdSet = new HashSet<>();
        for (AlertCatalogVo catalogVo : catalogList) {
            Long parentId = catalogVo.getParentId();
            if (parentId != null && !Objects.equals(parentId, catalogVo.getId())) {
                AlertCatalogVo parent = catalogMap.get(parentId);
                if (parent != null && !isAncestor(catalogMap, catalogVo.getId(), parentId)) {
                    parent.getChildren().add(catalogVo);
                    childIdSet.add(catalogVo.getId());
                }
            }
        }
        List<AlertCatalogVo> rootList = new ArrayList<>();
        for (AlertCatalogVo catalogVo : catalogList) {
            if (!childIdSet.contains(catalogVo.getId())) {
                rootList.add(catalogVo);
            }
        }
        sortTree(rootList);
        return rootList;
    }

    private boolean isAncestor(Map<Long, AlertCatalogVo> catalogMap, Long currentId, Long parentId) {
        Set<Long> visited = new HashSet<>();
        while (parentId != null && visited.add(parentId)) {
            if (Objects.equals(parentId, currentId)) {
                return true;
            }
            AlertCatalogVo parent = catalogMap.get(parentId);
            if (parent == null) {
                return false;
            }
            parentId = parent.getParentId();
        }
        return false;
    }

    private void sortTree(List<AlertCatalogVo> catalogList) {
        Collections.sort(catalogList, new Comparator<AlertCatalogVo>() {
            @Override
            public int compare(AlertCatalogVo o1, AlertCatalogVo o2) {
                return Integer.compare(o1.getSort(), o2.getSort());
            }
        });
        for (AlertCatalogVo catalogVo : catalogList) {
            if (CollectionUtils.isNotEmpty(catalogVo.getChildren())) {
                sortTree(catalogVo.getChildren());
            }
        }
    }
}
