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
import neatlogic.framework.alert.auth.ALERT_EXPORT;
import neatlogic.framework.alert.dto.AlertAttrDefineVo;
import neatlogic.framework.alert.dto.AlertAttrTypeVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertAttr;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.binarystream.PrivateBinaryStreamApiComponentBase;
import neatlogic.framework.util.$;
import neatlogic.framework.util.excel.ExcelBuilder;
import neatlogic.framework.util.excel.SheetBuilder;
import neatlogic.module.alert.attr.AttrFormaterFactory;
import neatlogic.module.alert.attr.IAttrFormater;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
@AuthAction(action = ALERT_EXPORT.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ExportAlertApi extends PrivateBinaryStreamApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(ExportAlertApi.class);
    private final ReentrantLock exportLock = new ReentrantLock();

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private IAlertService alertService;


    @Override
    public String getToken() {
        return "/alert/export";
    }

    @Override
    public String getName() {
        return "导出告警";
    }


    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", desc = "告警id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            @Param(name = "updateTimeHour", desc = "告警时间(小时)", type = ApiParamType.INTEGER),
            @Param(name = "status", desc = "状态", type = ApiParamType.STRING),
            @Param(name = "source", desc = "来源", type = ApiParamType.STRING),
            @Param(name = "level", desc = "级别", type = ApiParamType.INTEGER),
            @Param(name = "markNameList", desc = "标签列表", type = ApiParamType.JSONARRAY),
            @Param(name = "viewName", desc = "视图", type = ApiParamType.STRING),
            @Param(name = "attrFilterList", desc = "自定义属性过滤列表", type = ApiParamType.JSONARRAY),
            @Param(name = "rule", desc = "高级模式搜索条件", type = ApiParamType.JSONOBJECT),
            @Param(name = "showAttrList", desc = "需要的属性列表", type = ApiParamType.JSONARRAY, isRequired = true),
            @Param(name = "sortData", desc = "排序", type = ApiParamType.JSONOBJECT)
    })
    @Description(desc = "nmcac.exportcientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        AlertVo alertVo = JSON.toJavaObject(jsonObj, AlertVo.class);
        JSONArray showAttrList = jsonObj.getJSONArray("showAttrList");
        JSONArray idList = jsonObj.getJSONArray("idList");
        //导出告警使用flat搜索模式
        alertVo.setSearchMode("flat");

        List<String> headerList = new ArrayList<>();
        List<String> columnList = new ArrayList<>();
        List<AlertAttrDefineVo> attrList = AlertAttr.getColumnConstAttrList();
        List<AlertAttrTypeVo> alertAttrTypeList = alertAttrTypeMapper.listAttrType();
        for (int i = 0; i < showAttrList.size(); i++) {
            int finalI = i;
            Optional<AlertAttrDefineVo> constAttrOp = attrList.stream().filter(d -> Objects.equals(d.getName(), showAttrList.getString(finalI))).findFirst();
            if (constAttrOp.isPresent()) {
                headerList.add(constAttrOp.get().getLabel());
                columnList.add(constAttrOp.get().getName());
            } else {
                Optional<AlertAttrTypeVo> attrOp = alertAttrTypeList.stream().filter(d -> Objects.equals("attr_" + d.getName(), showAttrList.getString(finalI))).findFirst();
                if (attrOp.isPresent()) {
                    headerList.add(attrOp.get().getLabel());
                    columnList.add("attr_" + attrOp.get().getName());
                }
            }
        }

        ExcelBuilder builder = new ExcelBuilder(SXSSFWorkbook.class);
        SheetBuilder sheetBuilder = builder.withBorderColor(HSSFColor.HSSFColorPredefined.GREY_40_PERCENT)
                .withHeadFontColor(HSSFColor.HSSFColorPredefined.WHITE)
                .withHeadBgColor(HSSFColor.HSSFColorPredefined.GREY_80_PERCENT)
                .withColumnWidth(30)
                .addSheet($.t("common.data"))
                .withHeaderList(headerList)
                .withColumnList(columnList);
        Workbook workbook = builder.build();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileNameEncode = "告警数据" + sdf.format(new Date()) + ".xlsx";
        if (request.getHeader("User-Agent").toLowerCase().contains("msie") || request.getHeader("User-Agent").contains("Gecko")) {
            fileNameEncode = URLEncoder.encode(fileNameEncode, StandardCharsets.UTF_8);// IE浏览器
        } else {
            fileNameEncode = new String(fileNameEncode.replace(" ", "").getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        }
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        response.setHeader("Content-Disposition", " attachment; filename=\"" + fileNameEncode + "\"");


        //最多返回1000条数据
        int max = 1000;
        int count = 0;
        alertVo.setPageSize(100);
        List<AlertVo> alertList = alertService.searchAlert(alertVo);
        try (OutputStream os = response.getOutputStream()) {
            while (CollectionUtils.isNotEmpty(alertList)) {
                for (AlertVo alert : alertList) {
                    JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alert));
                    Map<String, Object> dataMap = new HashMap<>();
                    for (String column : columnList) {
                        if (column.startsWith("const_")) {
                            IAttrFormater formater = AttrFormaterFactory.getFormater(column);
                            dataMap.put(column, formater.getValue(column, alertObj));
                        } else if (column.startsWith("attr_")) {
                            if (MapUtils.isNotEmpty(alertObj.getJSONObject("attrObj"))) {
                                dataMap.put(column, alertObj.getJSONObject("attrObj").get(column.substring("attr_".length())));
                            }
                        }
                    }
                    sheetBuilder.addData(dataMap);
                    count += 1;
                }
                if (count >= max) {
                    break;
                }
                if (CollectionUtils.isEmpty(idList)) {
                    alertVo.setCurrentPage(alertVo.getCurrentPage() + 1);
                    alertList = alertService.searchAlert(alertVo);
                } else {
                    break;
                }
            }

            workbook.write(os);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (workbook != null) {
                ((SXSSFWorkbook) workbook).dispose(); // 清理内存缓存
                workbook.close();
            }
        }
        return null;
    }
}
