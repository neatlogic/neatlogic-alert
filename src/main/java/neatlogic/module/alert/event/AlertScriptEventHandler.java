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

package neatlogic.module.alert.event;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.alert.dto.*;
import neatlogic.framework.alert.event.AlertEventHandlerBase;
import neatlogic.framework.alert.event.AlertEventType;
import neatlogic.framework.alert.exception.alert.AlertNotFoundException;
import neatlogic.framework.alert.exception.alertevent.AlertEventHandlerTriggerException;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.store.elasticsearch.ElasticsearchIndexFactory;
import neatlogic.framework.store.elasticsearch.IElasticsearchIndex;
import neatlogic.framework.util.Md5Util;
import neatlogic.framework.util.UuidUtil;
import neatlogic.framework.util.javascript.JavascriptUtil;
import neatlogic.module.alert.dao.mapper.AlertAttrTypeMapper;
import neatlogic.module.alert.dao.mapper.AlertAuditMapper;
import neatlogic.module.alert.dao.mapper.AlertMapper;
import neatlogic.module.alert.dao.mapper.AlertRuleMapper;
import neatlogic.module.alert.utils.AlertRuleUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.script.ScriptException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AlertScriptEventHandler extends AlertEventHandlerBase {
    @Resource
    private AlertMapper alertMapper;

    @Resource
    private AlertAuditMapper alertAuditMapper;

    @Resource
    private AlertRuleMapper alertRuleMapper;

    @Resource
    private AlertAttrTypeMapper alertAttrTypeMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;

    @Override
    protected AlertVo myTrigger(AlertEventHandlerVo alertEventHandlerVo, AlertEventPluginVo alertEventPluginVo, AlertVo alertVo, AlertEventHandlerAuditVo alertEventHandlerAuditVo, AlertEventStatusVo alertEventStatusVo) {
        JSONObject config = alertEventHandlerVo.getConfig();
        int isAlertExists = alertMapper.checkAlertIsExists(alertVo.getId());
        if (isAlertExists == 0) {
            throw new AlertNotFoundException(alertVo.getId());
        }
        if (config == null) {
            config = new JSONObject();
        }
        JSONObject oldAlertObj = new JSONObject();
        oldAlertObj.put("title", alertVo.getTitle());
        oldAlertObj.put("source", alertVo.getSource());
        oldAlertObj.put("level", alertVo.getLevel());
        oldAlertObj.put("alertTime", alertVo.getAlertTime());
        if (CollectionUtils.isNotEmpty(alertVo.getUserList())) {
            oldAlertObj.put("userAccountList", alertVo.getUserList().stream().map(AlertUserVo::getUserAccount).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(alertVo.getTeamList())) {
            oldAlertObj.put("userTeamList", alertVo.getTeamList().stream().map(AlertTeamVo::getTeamName).collect(Collectors.toList()));
        }
        oldAlertObj.put("attrObj", alertVo.getAttrObj());
        JSONObject resultObj = new JSONObject();
        //旧告警数据，用于前端展示告警数据
        resultObj.put("oldAlertObj", oldAlertObj);
        //就告警对象，用于前端展示完整的告警信息
        resultObj.put("oldAlert", JSON.parseObject(JSON.toJSONString(alertVo)));
        try {
            String transformed = JavascriptUtil.transform(oldAlertObj, config.getString("script"));
            //转换后的告警数据，不一定能转换成json，因此先保存一份字符串
            resultObj.put("newAlertStr", transformed);
            JSONObject newAlertObj = JSON.parseObject(transformed);
            //新告警数据，用于前端展示告警数据
            resultObj.put("newAlertObj", newAlertObj);

            //计算更新属性
            List<AlertAuditVo> auditList = new ArrayList<>();
            AlertVo newAlertVo = new AlertVo();
            newAlertVo.setId(alertVo.getId());
            boolean hasChange = false;
            if (!Objects.equals(oldAlertObj.getString("title"), newAlertObj.getString("title"))) {
                newAlertVo.setTitle(newAlertObj.getString("title"));
                auditList.add(generateAudit(alertVo.getId(), "const_title", new JSONArray() {{
                    this.add(oldAlertObj.getString("title"));
                }}, new JSONArray() {{
                    this.add(newAlertObj.getString("title"));
                }}));
                hasChange = true;
            }
            if (!Objects.equals(oldAlertObj.getInteger("level"), newAlertObj.getInteger("level"))) {
                newAlertVo.setLevel(newAlertObj.getInteger("level"));
                auditList.add(generateAudit(alertVo.getId(), "const_level", new JSONArray() {{
                    this.add(oldAlertObj.getInteger("level"));
                }}, new JSONArray() {{
                    this.add(newAlertObj.getInteger("level"));
                }}));
                hasChange = true;
            }
            if (!Objects.equals(oldAlertObj.getString("source"), newAlertObj.getString("source"))) {
                newAlertVo.setSource(newAlertObj.getString("source"));
                auditList.add(generateAudit(alertVo.getId(), "const_source", new JSONArray() {{
                    this.add(oldAlertObj.getString("source"));
                }}, new JSONArray() {{
                    this.add(newAlertObj.getString("source"));
                }}));
                hasChange = true;
            }

            if (hasChange) {
                alertMapper.updateAlert(newAlertVo);
            }

            //比较扩展属性
            JSONObject oldAttrObj = oldAlertObj.getJSONObject("attrObj");
            JSONObject newAttrObj = newAlertObj.getJSONObject("attrObj");
            if (oldAttrObj == null) {
                oldAttrObj = new JSONObject();
            }
            if (newAttrObj == null) {
                newAttrObj = new JSONObject();
            }
            List<AlertAttrTypeVo> attrTypeList = alertAttrTypeMapper.listAttrType();
            boolean hasAttrChange = false;

            for (AlertAttrTypeVo attrTypeVo : attrTypeList) {
                if (!Objects.equals(oldAttrObj.get(attrTypeVo.getName()), newAttrObj.get(attrTypeVo.getName()))) {
                    JSONObject finalOldAttrObj = oldAttrObj;
                    JSONObject finalNewAttrObj = newAttrObj;
                    auditList.add(generateAudit(alertVo.getId(), "attr_" + attrTypeVo.getName(), new JSONArray() {{
                        this.add(finalOldAttrObj.getString(attrTypeVo.getName()));
                    }}, new JSONArray() {{
                        this.add(finalNewAttrObj.getString(attrTypeVo.getName()));
                    }}));
                    hasAttrChange = true;
                }
            }

            if (hasAttrChange) {
                hasChange = true;
                newAlertVo.setAttrObj(newAttrObj);
                alertMapper.saveAlertAttr(newAlertVo);
            }

            if (!compareJSONArray(oldAlertObj.getJSONArray("userAccountList"), newAlertObj.getJSONArray("userAccountList"))) {
                hasChange = true;
                JSONArray oldUserIdList = new JSONArray();
                JSONArray newUserIdList = new JSONArray();
                if (CollectionUtils.isNotEmpty(alertVo.getUserIdList())) {
                    oldUserIdList.addAll(alertVo.getUserIdList());
                }
                alertMapper.deleteAlertUserByAlertId(alertVo.getId());
                for (int i = 0; i < newAlertObj.getJSONArray("userAccountList").size(); i++) {
                    String userAccount = newAlertObj.getJSONArray("userAccountList").getString(i);
                    UserVo userVo = userMapper.getUserByUserId(userAccount);
                    if (userVo != null) {
                        AlertUserVo alertUserVo = new AlertUserVo();
                        alertUserVo.setUserId(userVo.getUuid());
                        alertUserVo.setAlertId(alertVo.getId());
                        newUserIdList.add(userVo.getUuid());
                        alertMapper.insertAlertUser(alertUserVo);
                    }
                }
                auditList.add(generateAudit(alertVo.getId(), "const_userList", oldUserIdList, newUserIdList));
            }

            if (!compareJSONArray(oldAlertObj.getJSONArray("teamNameList"), newAlertObj.getJSONArray("teamNameList"))) {
                hasChange = true;
                JSONArray oldTeamIdList = new JSONArray();
                JSONArray newTeamIdList = new JSONArray();
                if (CollectionUtils.isNotEmpty(alertVo.getTeamIdList())) {
                    oldTeamIdList.addAll(alertVo.getTeamIdList());
                }
                alertMapper.deleteAlertTeamByAlertId(alertVo.getId());
                for (int i = 0; i < newAlertObj.getJSONArray("teamNameList").size(); i++) {
                    String teamName = newAlertObj.getJSONArray("teamNameList").getString(i);
                    List<String> uuidList = teamMapper.getTeamUuidByName(teamName);
                    if (CollectionUtils.isNotEmpty(uuidList)) {
                        for (String s : uuidList) {
                            AlertTeamVo alertTeamVo = new AlertTeamVo();
                            newTeamIdList.add(s);
                            alertTeamVo.setTeamUuid(s);
                            alertTeamVo.setAlertId(alertVo.getId());
                            alertMapper.insertAlertTeam(alertTeamVo);
                        }
                    }
                }
                auditList.add(generateAudit(alertVo.getId(), "const_teamList", oldTeamIdList, newTeamIdList));
            }
            //写入audit
            if (CollectionUtils.isNotEmpty(auditList)) {
                for (AlertAuditVo alertAuditVo : auditList) {
                    alertAuditMapper.insertAlertAudit(alertAuditVo);
                }
            }
            //重新查询一次新的告警信息
            newAlertVo = getAlertById(alertVo.getId());
            //检查是否需要重新计算唯一键
           /* if (config.getIntValue("needRebuildUniqueKey") == 1) {
                generateUniqueKey(config, newAlertVo);
                hasChange = true;
            }*/

            if (hasChange) {
                IElasticsearchIndex<AlertVo> indexHandler = ElasticsearchIndexFactory.getIndex("ALERT");
                indexHandler.deleteDocument(alertVo.getId());
                indexHandler.createDocument(alertVo.getId());
            }
            //新告警对象，用于前端展示完整的告警信息
            resultObj.put("newAlert", JSON.parseObject(JSON.toJSONString(newAlertVo)));
            return newAlertVo;
        } catch (ScriptException e) {
            throw new AlertEventHandlerTriggerException(e);
        } finally {
            alertEventHandlerAuditVo.setResult(resultObj);
        }
    }

    private void generateUniqueKey(JSONObject config, AlertVo alertVo) {
        //根据唯一规则计算unique key
        if (CollectionUtils.isNotEmpty(config.getJSONArray("uniqueAttrList"))) {
            List<AlertRuleVo> ruleList = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(config.getJSONArray("ruleList"))) {
                List<Long> ruleIdList = new ArrayList<>();
                for (int i = 0; i < config.getJSONArray("ruleList").size(); i++) {
                    ruleIdList.add(config.getJSONArray("ruleList").getLong(i));
                }
                ruleList = alertRuleMapper.getAlertRuleByIdList(ruleIdList);
            }

            List<String> attrList = new ArrayList<>();
            for (int i = 0; i < config.getJSONArray("uniqueAttrList").size(); i++) {
                attrList.add(config.getJSONArray("uniqueAttrList").getJSONObject(i).getString("name"));
            }
            //按属性名排序，避免由于顺序不同导致结果不同
            attrList.sort(String::compareTo);
            String key = "";
            JSONObject alertObj = JSON.parseObject(JSON.toJSONString(alertVo));
            for (String attr : attrList) {
                if (attr.startsWith("const_")) {
                    if (StringUtils.isNotBlank(key)) {
                        key += "#";
                    }
                    String value = alertObj.getString(attr.substring("const_".length()));
                    List<AlertRuleVo> tmpRuleList = ruleList.stream().filter(d -> d.getAttrName().equals(attr)).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(tmpRuleList)) {
                        value = AlertRuleUtils.doRule(value, tmpRuleList);
                    }
                    key += value;
                } else if (attr.startsWith("attr_")) {
                    JSONObject attrObj = alertObj.getJSONObject("attrObj");
                    if (attrObj != null && attrObj.get(attr.substring("attr_".length())) != null) {
                        if (StringUtils.isNotBlank(key)) {
                            key += "#";
                        }
                        String value = attrObj.getString(attr.substring("attr_".length()));
                        List<AlertRuleVo> tmpRuleList = ruleList.stream().filter(d -> d.getAttrName().equals(attr)).collect(Collectors.toList());
                        if (CollectionUtils.isNotEmpty(tmpRuleList)) {
                            value = AlertRuleUtils.doRule(value, tmpRuleList);
                        }
                        key += value;
                    }
                }
            }
            //一定要判断，因为可能直接选唯一键作为唯一键，这时候就需要二次转换
            if (StringUtils.isNotBlank(key)) {
                if (!Md5Util.isMd5(key)) {
                    alertVo.setUniqueKey(Md5Util.encryptMD5(key));
                } else {
                    alertVo.setUniqueKey(key);
                }
            }
        }
        //如果uniqueKey
        if (StringUtils.isNotBlank(alertVo.getUniqueKey())) {
            if (!Md5Util.isMd5(alertVo.getUniqueKey())) {
                alertVo.setUniqueKey(Md5Util.encryptMD5(alertVo.getUniqueKey()));
            }
        } else {
            //如果没有uniquekey则随机生成一个
            alertVo.setUniqueKey(UuidUtil.randomUuid());
        }
    }

    private static boolean compareJSONArray(JSONArray arr1, JSONArray arr2) {
        if (arr1 == null || arr2 == null) return false;
        if (arr1.size() != arr2.size()) return false;

        Set<String> set1 = new HashSet<>();
        Set<String> set2 = new HashSet<>();

        for (int i = 0; i < arr1.size(); i++) {
            set1.add(arr1.getString(i).toLowerCase());
        }
        for (int i = 0; i < arr2.size(); i++) {
            set2.add(arr2.getString(i).toLowerCase());
        }
        return set1.equals(set2);
    }

    private AlertAuditVo generateAudit(Long alertId, String attrName, JSONArray oldValue, JSONArray newValue) {
        AlertAuditVo alertAuditVo = new AlertAuditVo(true);
        alertAuditVo.setAlertId(alertId);
        alertAuditVo.setAttrName(attrName);
        alertAuditVo.setOldValueList(oldValue);
        alertAuditVo.setNewValueList(newValue);
        return alertAuditVo;
    }

    private AlertVo getAlertById(Long alertId) {
        AlertVo newAlertVo = alertEventMapper.getAlertById(alertId);
        //补充完整的处理人信息和处理组信息
        newAlertVo.setUserList(alertEventMapper.getAlertUserByAlertId(alertId));
        newAlertVo.setTeamList(alertEventMapper.getAlertTeamByAlertId(alertId));

        return newAlertVo;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public int getSort() {
        return 11;
    }

    @Override
    public String getName() {
        return "SCRIPT";
    }

    @Override
    public String getLabel() {
        return "修改告警数据";
    }

    @Override
    public String getIcon() {
        return "tsfont-edit";
    }

    @Override
    public String getDescription() {
        return "通过脚本修改告警数据";
    }

    @Override
    public Set<String> supportEventTypes() {
        return new HashSet<String>() {{
            //this.add(AlertEventType.ALERT_INPUT.getName());不能放Input,如果告警不存在则没法更新
            this.add(AlertEventType.ALERT_SAVE.getName());
            this.add(AlertEventType.ALERT_CONVERGE_IN.getName());
            this.add(AlertEventType.ALERT_CONVERGE_OUT.getName());
            this.add(AlertEventType.ALERT_CLOSE.getName());
            this.add(AlertEventType.ALERT_STATUE_CHANGE.getName());
            this.add(AlertEventType.ALERT_OPEN.getName());
        }};
    }

    @Override
    public Set<String> supportParentHandler() {
        return new HashSet<String>() {{
            this.add("condition");
            this.add("interval");
        }};
    }


}
