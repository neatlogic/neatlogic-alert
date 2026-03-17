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
import neatlogic.framework.alert.auth.ALERT_ADMIN;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertSearchMode;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.dto.elasticsearch.IndexResultVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.service.AlertDeleteHandler;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AuthAction(action = ALERT_ADMIN.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteAlertApi extends PrivateApiComponentBase {
    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertDeleteHandler alertDeleteHandler;

    @Resource
    private IAlertService alertService;

    @Override
    public String getToken() {
        return "alert/delete";
    }

    @Override
    public String getName() {
        return "删除告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "idList", desc = "id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "searchParam", desc = "过滤参数，参考查询接口", type = ApiParamType.JSONOBJECT),
            @Param(name = "isDeleteChildAlert", defaultValue = "0", rule = "0,1", desc = "是否删除子告警", type = ApiParamType.INTEGER)})
    @Description(desc = "删除告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        Long alertId = jsonObj.getLong("id");
        JSONArray idList = jsonObj.getJSONArray("idList");
        JSONObject searchParam = jsonObj.getJSONObject("searchParam");
        if (alertId == null && CollectionUtils.isEmpty(idList) && MapUtils.isEmpty(searchParam)) {
            throw new ParamNotExistsException("id", "idList", "searchParam");
        }
        Integer isDeleteChildAlert = jsonObj.getInteger("isDeleteChildAlert");
        if (alertId != null) {
            alertService.deleteAlert(alertId, Objects.equals(1, isDeleteChildAlert));
        } else if (CollectionUtils.isNotEmpty(idList)) {
            List<Long> alertIdList = new ArrayList<>();
            for (int i = 0; i < idList.size(); i++) {
                alertIdList.add(idList.getLong(i));
            }
            alertService.deleteAlert(alertIdList, Objects.equals(1, isDeleteChildAlert));
        } else if (MapUtils.isNotEmpty(searchParam) && MapUtils.isNotEmpty(searchParam.getJSONObject("rule"))) {
            Long deleteBatch = System.currentTimeMillis();
            CachedThreadPool.execute(new NeatLogicThread("ALERT-DELETE-MATCH") {
                @Override
                protected void execute() {
                    int limit = 1000;
                    //由于数据量可能很大，需要分页删除，为了避免数据删除后导致分页异常，需要先对数据做标记，然后再查出标记好的数据来做真正的删除处理
                    AlertVo alertVo = JSON.toJavaObject(searchParam, AlertVo.class);
                    alertVo.setCurrentPage(1);
                    alertVo.setPageSize(limit);
                    alertVo.setSearchMode(AlertSearchMode.FLAT.getValue());
                    IElasticsearchDocument<AlertVo> index = ElasticsearchDocumentFactory.getIndex("ALERT");
                    IndexResultVo indexResultVo = index.searchDocument(alertVo, alertVo.getCurrentPage(), alertVo.getPageSize());
                    while (CollectionUtils.isNotEmpty(indexResultVo.getIdList())) {
                        List<Long> alertIdList = indexResultVo.getIdList().stream().map(Long::parseLong).collect(Collectors.toList());
                        AlertVo tmpAlertVo = new AlertVo();
                        tmpAlertVo.setIdList(alertIdList);
                        tmpAlertVo.setDeleteBatch(deleteBatch);
                        alertMapper.updateAlertIsDeleteByIdList(tmpAlertVo);
                        alertVo.setCurrentPage(alertVo.getCurrentPage() + 1);
                        indexResultVo = index.searchDocument(alertVo, alertVo.getCurrentPage(), alertVo.getPageSize());
                    }
                    //开始删除标记好的数据
                    Long currentId = 0L;
                    List<Long> alertIdList = alertMapper.getIsDeleteAlertIdList(currentId, deleteBatch, limit);
                    while (CollectionUtils.isNotEmpty(alertIdList)) {
                        alertDeleteHandler.submitDeleteTask(alertIdList);
                        currentId = alertIdList.get(alertIdList.size() - 1);
                        alertIdList = alertMapper.getIsDeleteAlertIdList(currentId, deleteBatch, limit);
                    }
                }
            });

        }
        return null;
    }


}
