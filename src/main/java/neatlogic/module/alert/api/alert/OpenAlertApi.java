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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_ADMIN;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.exception.alert.AlertHasNotAuthException;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.service.IAlertService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class OpenAlertApi extends PrivateApiComponentBase {

    @Resource
    private IAlertService alertService;

    @Resource
    private AlertMapper alertMapper;

    @Override
    public String getToken() {
        return "alert/open";
    }

    @Override
    public String getName() {
        return "打开告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "idList", desc = "id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "isCloseChildAlert", isRequired = true, rule = "0,1", desc = "是否关闭子告警", type = ApiParamType.INTEGER)
    })
    @Description(desc = "打开告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long alertId = jsonObj.getLong("id");
        JSONArray idList = jsonObj.getJSONArray("idList");
        if (alertId == null && CollectionUtils.isEmpty(idList)) {
            throw new ParamNotExistsException("id", "idList");
        }
        Integer isCloseChildAlert = jsonObj.getInteger("isCloseChildAlert");
        if (alertId != null) {
            AlertVo alertVo = alertMapper.getAlertById(alertId);
            if (alertVo == null) {
                throw new AlertNotFoundException(alertId);
            }
            if (hasRole(alertVo)) {
                alertVo.setIsCloseChildAlert(isCloseChildAlert);
                alertService.openAlert(alertVo);
            } else {
                throw new AlertHasNotAuthException();
            }
        } else if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                Long id = idList.getLong(i);
                AlertVo alertVo = alertMapper.getAlertById(id);
                if (alertVo == null) {
                    throw new AlertNotFoundException(id);
                }
                if (hasRole(alertVo)) {
                    alertVo.setIsCloseChildAlert(isCloseChildAlert);
                    alertService.openAlert(alertVo);
                }
            }
        }
        return null;
    }

    private boolean hasRole(AlertVo alertVo) {
        boolean hasRole = AuthActionChecker.check(ALERT_ADMIN.class);
        if (!hasRole && CollectionUtils.isNotEmpty(alertVo.getUserList())) {
            hasRole = alertVo.getUserList().stream().anyMatch(d -> d.getUserId().equals(UserContext.get().getUserUuid(true)));
        }
        if (!hasRole && CollectionUtils.isNotEmpty(alertVo.getTeamIdList())) {
            List<String> userTeamList = UserContext.get().getTeamUuidList();
            hasRole = alertVo.getTeamList().stream().anyMatch(d -> userTeamList.contains(d.getTeamUuid()));
        }
        return hasRole;
    }
}
