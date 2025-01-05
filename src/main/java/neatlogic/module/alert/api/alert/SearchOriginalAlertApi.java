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

package neatlogic.module.alert.api.alert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.OriginalAlertVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchOriginalAlertApi extends PrivateApiComponentBase {

    @Resource
    private AlertMapper alertMapper;

    @Override
    public String getToken() {
        return "/alert/origin/search";
    }

    @Override
    public String getName() {
        return "搜索接入记录";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            @Param(name = "status", desc = "状态", rule = "succeed,failed", type = ApiParamType.STRING),
            @Param(name = "timeRange", desc = "时间范围", type = ApiParamType.JSONARRAY)
    })
    @Description(desc = "搜索接入记录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        OriginalAlertVo originalAlertVo = JSON.toJavaObject(jsonObj, OriginalAlertVo.class);
        List<OriginalAlertVo> alertList = alertMapper.searchAlertOrigin(originalAlertVo);
        if (CollectionUtils.isNotEmpty(alertList)) {
            int rowNum = alertMapper.searchAlertOriginCount(originalAlertVo);
            originalAlertVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(alertList, originalAlertVo);
    }
}
