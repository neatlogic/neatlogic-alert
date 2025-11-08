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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.dto.AlertViewVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.exception.alertview.AlertViewNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertViewMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
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
public class SearchAlertApi extends PrivateApiComponentBase {

    @Resource
    private IAlertService alertService;

    @Resource
    private AlertViewMapper alertViewMapper;

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Override
    public String getToken() {
        return "/alert/search";
    }

    @Override
    public String getName() {
        return "搜索告警";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({
            @Param(name = "fromAlertId", desc = "来源告警id", type = ApiParamType.LONG),
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            @Param(name = "updateTimeHour", desc = "告警时间(小时)", type = ApiParamType.INTEGER),
            @Param(name = "status", desc = "状态", type = ApiParamType.STRING),
            @Param(name = "source", desc = "来源", type = ApiParamType.STRING),
            @Param(name = "level", desc = "级别", type = ApiParamType.INTEGER),
            @Param(name = "markNameList", desc = "标签列表", type = ApiParamType.JSONARRAY),
            @Param(name = "viewName", desc = "视图", type = ApiParamType.STRING),
            @Param(name = "attrFilterList", desc = "自定义属性过滤列表", type = ApiParamType.JSONARRAY),
            @Param(name = "rule", desc = "高级模式搜索条件", type = ApiParamType.JSONOBJECT),
            @Param(name = "sortData", desc = "排序", type = ApiParamType.JSONOBJECT),
            @Param(name = "searchMode", desc = "搜索模式", rule = "tree,flat", defaultValue = "tree", type = ApiParamType.STRING),
    })
    @Description(desc = "搜索告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertVo alertVo = JSON.toJavaObject(jsonObj, AlertVo.class);
        if (StringUtils.isNotBlank(alertVo.getViewName())) {
            AlertViewVo alertViewVo = alertViewMapper.getAlertViewByName(alertVo.getViewName());
            if (alertViewVo == null) {
                throw new AlertViewNotFoundException(alertVo.getViewName());
            }
        }
        List<AlertVo> alertList = alertService.searchAlert(alertVo);
        JSONArray theadList = new JSONArray();
        List<AlertAttrDefineVo> attrList = AlertAttr.getColumnConstAttrList();
        List<AlertAttrTypeVo> alertAttrTypeList = alertAttrTypeMapper.listAttrType();
        boolean hasExtend = false;
        List<String> extendAttrKeyList = new ArrayList<>();
        if (StringUtils.isNotBlank(alertVo.getViewName())) {
            AlertViewVo alertViewVo = alertViewMapper.getAlertViewByName(alertVo.getViewName());
            if (MapUtils.isNotEmpty(alertViewVo.getConfig()) && alertViewVo.getConfig().containsKey("attrList")) {
                for (int i = 0; i < alertViewVo.getConfig().getJSONArray("attrList").size(); i++) {
                    String attr = alertViewVo.getConfig().getJSONArray("attrList").getString(i);
                    if (attr.startsWith("const_")) {
                        Optional<AlertAttrDefineVo> op = attrList.stream().filter(d -> d.getName().equals(attr)).findAny();
                        op.ifPresent(valueTextVo -> theadList.add(new JSONObject() {{
                            this.put("key", valueTextVo.getName());
                            this.put("title", valueTextVo.getLabel());
                            this.put("sort", valueTextVo.getIsSort());
                        }}));
                    } else if (attr.startsWith("attr_")) {
                        Optional<AlertAttrTypeVo> op = alertAttrTypeList.stream().filter(d -> d.getName().equals(attr.replace("attr_", ""))).findAny();
                        if (op.isPresent()) {
                            if (Objects.equals(1, op.get().getIsNormal())) {
                                theadList.add(new JSONObject() {{
                                    this.put("key", "attr_" + op.get().getName());
                                    this.put("title", op.get().getLabel());
                                }});
                            } else {
                                extendAttrKeyList.add("attr_" + op.get().getName());
                                if (!hasExtend) {
                                    theadList.add(new JSONObject() {{
                                        this.put("key", "const_attrObj");
                                        this.put("title", "扩展属性");
                                        this.put("attrList", extendAttrKeyList);
                                    }});
                                    hasExtend = true;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (AlertAttrDefineVo attr : attrList) {
                //所有告警默认去掉id和uniqueKey这两个属性
                if (!attr.getName().equals("const_id") && !attr.getName().equals("const_uniqueKey")) {
                    theadList.add(new JSONObject() {{
                        this.put("key", attr.getName());
                        this.put("title", attr.getLabel());
                        this.put("sort", attr.getIsSort());
                    }});
                }
            }
            for (AlertAttrTypeVo alertAttrType : alertAttrTypeList) {
                if (Objects.equals(1, alertAttrType.getIsNormal())) {
                    theadList.add(new JSONObject() {{
                        this.put("key", "attr_" + alertAttrType.getName());
                        this.put("title", alertAttrType.getLabel());
                    }});
                } else {
                    extendAttrKeyList.add("attr_" + alertAttrType.getName());
                }
            }
            //没有视图情况下扩展属性永远在最后显示
            if (CollectionUtils.isNotEmpty(extendAttrKeyList)) {
                theadList.add(new JSONObject() {{
                    this.put("key", "const_attrObj");
                    this.put("title", "扩展属性");
                    this.put("attrList", extendAttrKeyList);
                }});
            }
        }
        return TableResultUtil.getResult(theadList, alertList, alertVo);
    }
}
