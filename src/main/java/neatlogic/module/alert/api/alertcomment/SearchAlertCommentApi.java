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

package neatlogic.module.alert.api.alertcomment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertCommentVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertCommentMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertCommentApi extends PrivateApiComponentBase {

    @Resource
    private AlertCommentMapper alertCommentMapper;


    @Override
    public String getToken() {
        return "/alert/comment/search";
    }

    @Override
    public String getName() {
        return "搜索告警评论";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "alertId", desc = "告警id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "currentPage", desc = "当前页", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "每页大小", type = ApiParamType.INTEGER)
    })
    @Output({@Param(explode = AlertCommentVo[].class)})
    @Description(desc = "搜索告警评论")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertCommentVo alertCommentVo = JSON.toJavaObject(jsonObj, AlertCommentVo.class);
        int rowNum = alertCommentMapper.searchAlertCommentCount(alertCommentVo);
        alertCommentVo.setRowNum(rowNum);
        return TableResultUtil.getResult(alertCommentMapper.searchAlertComment(alertCommentVo), alertCommentVo);
    }
}
