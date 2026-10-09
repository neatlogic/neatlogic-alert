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
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.alert.exception.alertview.AlertViewNotFoundException;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.$;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertAllAlertConfigMapper;
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

    @Resource
    private AlertAllAlertConfigMapper alertAllAlertConfigMapper;

    @Override
    public String getToken() {
        return "/alert/search";
    }

    @Override
    public String getName() {
        return "搜索告警";
    }

    /** 为 MCP 工具列表和接口帮助提供一致的检索说明。 */
    @Override
    public String getDescription() {
        return "nmaa.searchalertapi.description";
    }

    @Override
    public String getConfig() {
        return null;
    }


    @Input({
            @Param(name = "fromAlertId", desc = "nmaa.searchalertapi.input.fromalertid.desc", help = "nmaa.searchalertapi.input.fromalertid.help", type = ApiParamType.LONG),
            @Param(name = "keyword", desc = "nmaa.searchalertapi.input.keyword.desc", help = "nmaa.searchalertapi.input.keyword.help", type = ApiParamType.STRING),
            @Param(name = "updateTimeHour", desc = "nmaa.searchalertapi.input.updatetimehour.desc", help = "nmaa.searchalertapi.input.updatetimehour.help", type = ApiParamType.INTEGER),
            @Param(name = "status", desc = "nmaa.searchalertapi.input.status.desc", help = "nmaa.searchalertapi.input.status.help", type = ApiParamType.STRING),
            @Param(name = "statusList", desc = "nmaa.searchalertapi.input.statuslist.desc", help = "nmaa.searchalertapi.input.statuslist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "source", desc = "nmaa.searchalertapi.input.source.desc", help = "nmaa.searchalertapi.input.source.help", type = ApiParamType.STRING),
            @Param(name = "sourceList", desc = "nmaa.searchalertapi.input.sourcelist.desc", help = "nmaa.searchalertapi.input.sourcelist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "level", desc = "nmaa.searchalertapi.input.level.desc", help = "nmaa.searchalertapi.input.level.help", type = ApiParamType.INTEGER),
            @Param(name = "levelList", desc = "nmaa.searchalertapi.input.levellist.desc", help = "nmaa.searchalertapi.input.levellist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "markNameList", desc = "nmaa.searchalertapi.input.marknamelist.desc", help = "nmaa.searchalertapi.input.marknamelist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "teamIdList", desc = "nmaa.searchalertapi.input.teamidlist.desc", help = "nmaa.searchalertapi.input.teamidlist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "viewName", desc = "nmaa.searchalertapi.input.viewname.desc", help = "nmaa.searchalertapi.input.viewname.help", type = ApiParamType.STRING),
            @Param(name = "attrFilterList", desc = "nmaa.searchalertapi.input.attrfilterlist.desc", help = "nmaa.searchalertapi.input.attrfilterlist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "rule", desc = "nmaa.searchalertapi.input.rule.desc", help = "nmaa.searchalertapi.input.rule.help", type = ApiParamType.JSONOBJECT),
            @Param(name = "sortData", desc = "nmaa.searchalertapi.input.sortdata.desc", help = "nmaa.searchalertapi.input.sortdata.help", type = ApiParamType.JSONOBJECT),
            @Param(name = "searchMode", desc = "nmaa.searchalertapi.input.searchmode.desc", help = "nmaa.searchalertapi.input.searchmode.help", rule = "tree,flat", defaultValue = "tree", type = ApiParamType.STRING),
            @Param(name = "currentPage", desc = "nmaa.searchalertapi.input.currentpage.desc", help = "nmaa.searchalertapi.input.currentpage.help", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "nmaa.searchalertapi.input.pagesize.desc", help = "nmaa.searchalertapi.input.pagesize.help", type = ApiParamType.INTEGER)
    })
    @Output({
            @Param(name = "theadList", desc = "nmaa.searchalertapi.output.theadlist.desc", help = "nmaa.searchalertapi.output.theadlist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "tbodyList", desc = "nmaa.searchalertapi.output.tbodylist.desc", help = "nmaa.searchalertapi.output.tbodylist.help", type = ApiParamType.JSONARRAY),
            @Param(name = "currentPage", desc = "nmaa.searchalertapi.output.currentpage.desc", help = "nmaa.searchalertapi.output.currentpage.help", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "nmaa.searchalertapi.output.pagesize.desc", help = "nmaa.searchalertapi.output.pagesize.help", type = ApiParamType.INTEGER),
            @Param(name = "pageCount", desc = "nmaa.searchalertapi.output.pagecount.desc", help = "nmaa.searchalertapi.output.pagecount.help", type = ApiParamType.INTEGER),
            @Param(name = "rowNum", desc = "nmaa.searchalertapi.output.rownum.desc", help = "nmaa.searchalertapi.output.rownum.help", type = ApiParamType.INTEGER)
    })
    @Description(desc = "nmaa.searchalertapi.description")
    @Example(title = "nmaa.searchalertapi.example.recent.title", description = "nmaa.searchalertapi.example.recent.description",
            example = "{\"keyword\":\"CPU\",\"updateTimeHour\":24,\"searchMode\":\"flat\",\"currentPage\":1,\"pageSize\":20,\"sortData\":{\"const_updateTime\":\"desc\"}}")
    @Example(title = "nmaa.searchalertapi.example.roots.title", description = "nmaa.searchalertapi.example.roots.description",
            example = "{\"searchMode\":\"tree\",\"currentPage\":1,\"pageSize\":20}")
    @Example(title = "nmaa.searchalertapi.example.children.title", description = "nmaa.searchalertapi.example.children.description",
            example = "{\"searchMode\":\"tree\",\"fromAlertId\":10001,\"currentPage\":1,\"pageSize\":20}")
    @Example(title = "nmaa.searchalertapi.example.open.title", description = "nmaa.searchalertapi.example.open.description",
            example = "{\"searchMode\":\"flat\",\"rule\":{\"conditionGroupList\":[{\"conditionList\":[{\"id\":\"const_isClose\",\"expression\":\"equal\",\"valueList\":[\"0\"]},{\"id\":\"const_title\",\"expression\":\"like\",\"valueList\":[\"CPU\"]}],\"conditionRelList\":[\"and\"]}],\"conditionGroupRelList\":[]},\"currentPage\":1,\"pageSize\":20}")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertVo alertVo = JSON.toJavaObject(jsonObj, AlertVo.class);
        JSONObject alertViewConfig = null;
        if (StringUtils.isNotBlank(alertVo.getViewName())) {
            AlertViewVo alertViewVo = alertViewMapper.getAlertViewByName(alertVo.getViewName());
            if (alertViewVo == null) {
                throw new AlertViewNotFoundException(alertVo.getViewName());
            }
            alertViewConfig = alertViewVo.getConfig();
        } else {
            AlertAllAlertConfigVo configVo = alertAllAlertConfigMapper.getAlertAllAlertConfigByName("all");
            if (configVo != null) {
                alertViewConfig = configVo.getConfig();
            }
        }
        List<AlertVo> alertList = alertService.searchAlert(alertVo);
        JSONArray theadList = new JSONArray();
        List<AlertAttrDefineVo> attrList = AlertAttr.getColumnConstAttrList();
        List<AlertAttrTypeVo> alertAttrTypeList = alertAttrTypeMapper.listAttrType();
        boolean hasExtend = false;
        List<String> extendAttrKeyList = new ArrayList<>();
        if (MapUtils.isNotEmpty(alertViewConfig) && alertViewConfig.containsKey("attrList")) {
            for (int i = 0; i < alertViewConfig.getJSONArray("attrList").size(); i++) {
                String attr = alertViewConfig.getJSONArray("attrList").getString(i);
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
                                    this.put("title", $.t("term.alert.extendattr"));
                                    this.put("attrList", extendAttrKeyList);
                                }});
                                hasExtend = true;
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
                if (Objects.equals(1, alertAttrType.getIsShow())) {
                    if (Objects.equals(1, alertAttrType.getIsNormal())) {
                        theadList.add(new JSONObject() {{
                            this.put("key", "attr_" + alertAttrType.getName());
                            this.put("title", alertAttrType.getLabel());
                        }});
                    } else {
                        extendAttrKeyList.add("attr_" + alertAttrType.getName());
                    }
                }
            }
            //没有视图情况下扩展属性永远在最后显示
            if (CollectionUtils.isNotEmpty(extendAttrKeyList)) {
                theadList.add(new JSONObject() {{
                    this.put("key", "const_attrObj");
                    this.put("title", $.t("term.alert.extendattr"));
                    this.put("attrList", extendAttrKeyList);
                }});
            }
        }
        return TableResultUtil.getResult(theadList, alertList, alertVo);
    }
}
