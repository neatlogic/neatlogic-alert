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

package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.attr.freemarker.AlertAttrFreemarkerSnippetFactory;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.exception.alertview.AlertViewNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAlertAttrApi extends PrivateApiComponentBase {

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private AlertViewMapper alertViewMapper;

    @Override
    public String getToken() {
        return "/alert/attr/list";
    }

    @Override
    public String getName() {
        return "返回告警属性列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "viewId", desc = "视图id", type = ApiParamType.LONG),
            @Param(name = "viewName", desc = "视图唯一标识", type = ApiParamType.STRING),
            @Param(name = "kind", desc = "属性大类", rule = "const,attr", type = ApiParamType.STRING),
            @Param(name = "isExpand", desc = "是否展开", rule = "0,1", type = ApiParamType.INTEGER),
            @Param(name = "isCondition", desc = "是否用于条件判断", rule = "0,1", type = ApiParamType.INTEGER),
            @Param(name = "isColumn", desc = "是否用于展示", rule = "0,1", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = AlertAttrDefineVo[].class)})
    @Description(desc = "返回告警属性列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        Long viewId = jsonObj.getLong("viewId");
        String viewName = jsonObj.getString("viewName");
        String kind = jsonObj.getString("kind");
        int isExpand = jsonObj.getIntValue("isExpand");
        int isCondition = jsonObj.getIntValue("isCondition");
        int isColumn = jsonObj.getIntValue("isColumn");
        int isSearch = 0;
        List<AlertAttrDefineVo> attrList = new ArrayList<>();
        if (StringUtils.isBlank(kind) || kind.equalsIgnoreCase("const")) {
            //TODO 逻辑是对的，后面再修改一下写法
            if (isCondition == 1) {
                attrList.addAll(AlertAttr.getConditionConstAttrList());
            } else if (isExpand == 1) {
                attrList.addAll(AlertAttr.getTemplateConstAttrList());
            } else if (isColumn == 1) {
                attrList.addAll(AlertAttr.getColumnConstAttrList());
            } else {
                attrList.addAll(AlertAttr.getSearchConstAttrList());
                isSearch = 1;
            }
        }
        if (StringUtils.isBlank(kind) || kind.equalsIgnoreCase("attr")) {
            List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
            for (AlertAttrTypeVo attrTypeVo : attrTypeList) {
                if (isSearch == 1 && !Objects.equals(attrTypeVo.getIsIndex(), 1)) {
                    continue;
                }
                if (isColumn == 1 && !Objects.equals(attrTypeVo.getIsShow(), 1)) {
                    continue;
                }
                attrList.add(new AlertAttrDefineVo()
                        .setId(attrTypeVo.getId())
                        .setName("attr_" + attrTypeVo.getName())
                        .setLabel(attrTypeVo.getLabel())
                        .setKind("attr")
                        .setType(attrTypeVo.getType())
                        .setExpressionList(attrTypeVo.getExpressionList())
                        .setConfig(attrTypeVo.getConfig())
                        .setFreemarkerSnippet(AlertAttrFreemarkerSnippetFactory.getFreemarkerSnippet(attrTypeVo))
                        .setWholeRow(Objects.equals(1, attrTypeVo.getIsRow()))
                        .setIsTab(Objects.equals(1, attrTypeVo.getIsTab()))
                        .setIsTop(attrTypeVo.getIsTop()));
            }
        }
        AlertViewVo alertViewVo = null;
        if (viewId != null) {
            alertViewVo = alertViewMapper.getAlertViewById(viewId);
            if (alertViewVo == null) {
                throw new AlertViewNotFoundException(viewId);
            }
        } else if (StringUtils.isNotBlank(viewName)) {
            alertViewVo = alertViewMapper.getAlertViewByName(viewName);
            if (alertViewVo == null) {
                throw new AlertViewNotFoundException(viewName);
            }
        }
        if (alertViewVo != null) {
            List<AlertAttrDefineVo> finalAttrList = new ArrayList<>();
            if (MapUtils.isNotEmpty(alertViewVo.getConfig()) && alertViewVo.getConfig().containsKey("attrList")) {
                for (int i = 0; i < alertViewVo.getConfig().getJSONArray("attrList").size(); i++) {
                    String attr = alertViewVo.getConfig().getJSONArray("attrList").getString(i);
                    Optional<AlertAttrDefineVo> op = attrList.stream().filter(d -> d.getName().equals(attr)).findAny();
                    op.ifPresent(finalAttrList::add);
                }
            }
            return finalAttrList;
        }
        return attrList;
    }

}
