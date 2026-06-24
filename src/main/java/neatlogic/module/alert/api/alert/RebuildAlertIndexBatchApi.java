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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_INDEX;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.store.elasticsearch.ElasticsearchDocumentFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = ALERT_INDEX.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class RebuildAlertIndexBatchApi extends PrivateApiComponentBase {
    private final Logger logger = LoggerFactory.getLogger(RebuildAlertIndexBatchApi.class);

    @Override
    public String getToken() {
        return "alert/index/rebuild/batch";
    }

    @Override
    public String getName() {
        return "批量重建告警索引";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "idList", desc = "告警id列表", isRequired = true, type = ApiParamType.JSONARRAY)
    })
    @Description(desc = "批量重建告警索引")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        if (CollectionUtils.isEmpty(idList)) {
            throw new ParamNotExistsException("idList");
        }
        List<Long> alertIdList = new ArrayList<>();
        for (int i = 0; i < idList.size(); i++) {
            alertIdList.add(idList.getLong(i));
        }
        // 批量重建可能耗时较长，接口只负责提交后台任务，避免前端等待每条索引重建完成。
        CachedThreadPool.execute(new NeatLogicThread("ALERT-REBUILD-INDEX-BATCH") {
            @Override
            @SuppressWarnings("unchecked")
            protected void execute() {
                IElasticsearchDocument<AlertVo> index = ElasticsearchDocumentFactory.getIndex("ALERT");
                for (Long id : alertIdList) {
                    try {
                        index.deleteDocument(id);
                        index.createDocument(id);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        return null;
    }
}
