package neatlogic.module.alert.api.breaker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.auth.ALERT_BASE;
import neatlogic.framework.alert.dao.mapper.AlertBreakerMapper;
import neatlogic.framework.alert.dto.breaker.AlertBreakerStateVo;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = ALERT_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAlertBreakerStateApi extends PrivateApiComponentBase {
    @Resource
    private AlertBreakerMapper alertBreakerMapper;

    @Override
    public String getToken() {
        return "/alert/breaker/state/search";
    }

    @Override
    public String getName() {
        return "搜索告警熔断状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "policyId", desc = "策略id", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "currentPage", desc = "common.currentpage", type = ApiParamType.INTEGER),
            @Param(name = "pageSize", desc = "common.pagesize", type = ApiParamType.INTEGER)
    })
    @Description(desc = "搜索告警熔断状态")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        AlertBreakerStateVo stateVo = JSON.toJavaObject(jsonObj, AlertBreakerStateVo.class);
        int rowNum = alertBreakerMapper.searchAlertBreakerStateCount(stateVo);
        if (rowNum > 0) {
            stateVo.setRowNum(rowNum);
        }
        return TableResultUtil.getResult(alertBreakerMapper.searchAlertBreakerState(stateVo), stateVo);
    }
}
