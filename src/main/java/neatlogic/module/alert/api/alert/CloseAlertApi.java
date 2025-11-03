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
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.core.ApiRuntimeException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
@Transactional
public class CloseAlertApi extends PrivateApiComponentBase {
    private final Logger logger = LoggerFactory.getLogger(CloseAlertApi.class);
    @Resource
    private IAlertService alertService;

    @Resource
    private AlertMapper alertMapper;

    @Override
    public String getToken() {
        return "alert/close";
    }

    @Override
    public String getName() {
        return "关闭告警";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "id", type = ApiParamType.LONG),
            @Param(name = "idList", desc = "id列表", type = ApiParamType.JSONARRAY),
            @Param(name = "isAll", rule = "0,1", desc = "是否全部关闭，输入1或0", type = ApiParamType.INTEGER),
            @Param(name = "isCloseChildAlert", rule = "0,1", desc = "是否关闭子告警", type = ApiParamType.INTEGER)
    })
    @Description(desc = "关闭告警")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long alertId = jsonObj.getLong("id");
        JSONArray idList = jsonObj.getJSONArray("idList");
        Integer isAll = jsonObj.getInteger("isAll");
        if (alertId == null && CollectionUtils.isEmpty(idList) && isAll == null) {
            throw new ParamNotExistsException("id", "idList", "isAll");
        }
        Integer isCloseChildAlert = jsonObj.getInteger("isCloseChildAlert");
        if (isCloseChildAlert == null) {
            isCloseChildAlert = 0;
        }
        if (alertId != null) {
            AlertVo alertVo = alertMapper.getAlertById(alertId);
            if (alertVo == null) {
                throw new AlertNotFoundException(alertId);
            }

            if (hasRole(alertVo)) {
                alertVo.setIsCloseChildAlert(isCloseChildAlert);
            } else {
                throw new AlertHasNotAuthException();
            }
            alertService.closeAlert(alertVo);
        } else if (CollectionUtils.isNotEmpty(idList)) {
            for (int i = 0; i < idList.size(); i++) {
                Long id = idList.getLong(i);
                AlertVo alertVo = alertMapper.getAlertById(id);
                if (alertVo == null) {
                    throw new AlertNotFoundException(id);
                }
                if (hasRole(alertVo)) {
                    alertVo.setIsCloseChildAlert(isCloseChildAlert);
                    alertService.closeAlert(alertVo);
                }
            }
        } else if (Objects.equals(1, isAll)) {
            if (AuthActionChecker.check(ALERT_ADMIN.class)) {
                CachedThreadPool.execute(new NeatLogicThread("CLOSE_ALL_ALERT") {
                    @Override
                    protected void execute() {
                        AlertVo paramAlertVo = new AlertVo();
                        paramAlertVo.setPageSize(100);
                        List<Long> idList = alertMapper.getAllOpenAlertId(paramAlertVo);
                        while (CollectionUtils.isNotEmpty(idList)) {
                            for (Long id : idList) {
                                AlertVo alertVo = alertMapper.getAlertById(id);
                                alertVo.setIsClose(0);
                                alertVo.setIsCloseChildAlert(0);
                                try {
                                    alertService.closeAlert(alertVo);
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                                //切换id基线
                                paramAlertVo.setId(id);
                            }
                            idList = alertMapper.getAllOpenAlertId(paramAlertVo);
                        }
                    }
                });
            } else {
                throw new ApiRuntimeException("没有权限");
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
