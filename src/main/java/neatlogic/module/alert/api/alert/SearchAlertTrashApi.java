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
import neatlogic.framework.alert.dto.AlertTrashVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.alert.service.IAlertService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertTrashApi extends PrivateApiComponentBase {

    @Resource
    private IAlertService alertService;


    @Override
    public String getToken() {
        return "/alerttrash/search";
    }

    @Override
    public String getName() {
        return "搜索已删除告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", desc = "关键字", type = ApiParamType.STRING),
            @Param(name = "updateTimeHour", desc = "告警时间(小时)", type = ApiParamType.INTEGER),
            @Param(name = "deleteTimeHour", desc = "删除时间(小时)", type = ApiParamType.INTEGER),
            @Param(name = "status", desc = "状态", type = ApiParamType.STRING),
            @Param(name = "source", desc = "来源", type = ApiParamType.STRING),
            @Param(name = "level", desc = "级别", type = ApiParamType.INTEGER)
    })
    @Description(desc = "搜索已删除告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        AlertTrashVo alertVo = JSON.toJavaObject(jsonObj, AlertTrashVo.class);
        List<AlertTrashVo> alertList = alertService.searchAlertTrash(alertVo);
        return TableResultUtil.getResult(alertList, alertVo);
    }
}
