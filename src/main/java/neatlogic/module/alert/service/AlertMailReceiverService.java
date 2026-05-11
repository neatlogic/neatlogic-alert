package neatlogic.module.alert.service;

import com.alibaba.fastjson.JSONArray;
import neatlogic.framework.alert.dao.mapper.AlertEventMapper;
import neatlogic.framework.alert.dto.AlertTeamVo;
import neatlogic.framework.alert.dto.AlertUserVo;
import neatlogic.framework.alert.dto.AlertVo;
import neatlogic.framework.alert.enums.AlertUserType;
import neatlogic.framework.common.constvalue.AuthType;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.TeamVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.module.alert.dto.AlertMailReceiverVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AlertMailReceiverService {
    @Resource
    private AlertEventMapper alertEventMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeamMapper teamMapper;

    public AlertMailReceiverVo getReceiver(AlertVo alertVo, JSONArray toUserList, JSONArray ccUserList) {
        AlertMailReceiverVo receiverVo = new AlertMailReceiverVo();
        receiverVo.setToList(makeupMailList(alertVo, toUserList));
        receiverVo.setCcList(makeupMailList(alertVo, ccUserList));
        receiverVo.setGroupName(makeupGroupName(alertVo, toUserList, ccUserList, receiverVo));
        return receiverVo;
    }

    private Set<String> makeupMailList(AlertVo alertVo, JSONArray userList) {
        Set<String> mailSet = new HashSet<>();
        if (CollectionUtils.isEmpty(userList)) {
            return mailSet;
        }
        for (int i = 0; i < userList.size(); i++) {
            String userUuid = userList.getString(i);
            if (("alertUserType#" + AlertUserType.WORKER.getValue()).equals(userUuid)) {
                addWorkerMail(alertVo, mailSet);
            } else if (("alertUserType#" + AlertUserType.WORKER_TEAM.getValue()).equals(userUuid)) {
                addWorkerTeamMail(alertVo, mailSet);
            } else if (("alertUserType#" + AlertUserType.WORKER_TEAM_USER.getValue()).equals(userUuid)) {
                addWorkerTeamUserMail(alertVo, mailSet);
            } else if (userUuid.startsWith("user#")) {
                UserVo userVo = userMapper.getUserByUuid(AuthType.removePrefix(userUuid));
                if (userVo != null && StringUtils.isNotBlank(userVo.getEmail())) {
                    mailSet.add(userVo.getEmail());
                }
            } else if (userUuid.startsWith("team#")) {
                TeamVo teamVo = teamMapper.getTeamByUuid(AuthType.removePrefix(userUuid));
                if (teamVo != null && StringUtils.isNotBlank(teamVo.getEmail())) {
                    mailSet.add(teamVo.getEmail());
                }
            }
        }
        return mailSet;
    }

    private void addWorkerMail(AlertVo alertVo, Set<String> mailSet) {
        List<AlertUserVo> alertUserList = alertVo.getUserList();
        if (CollectionUtils.isEmpty(alertUserList)) {
            alertUserList = alertEventMapper.getAlertUserByAlertId(alertVo.getId());
        }
        if (CollectionUtils.isNotEmpty(alertUserList)) {
            for (AlertUserVo user : alertUserList) {
                if (StringUtils.isNotBlank(user.getUserEmail())) {
                    mailSet.add(user.getUserEmail());
                }
            }
        }
    }

    private void addWorkerTeamMail(AlertVo alertVo, Set<String> mailSet) {
        List<AlertTeamVo> alertTeamList = alertVo.getTeamList();
        if (CollectionUtils.isEmpty(alertTeamList)) {
            alertTeamList = alertEventMapper.getAlertTeamByAlertId(alertVo.getId());
        }
        if (CollectionUtils.isNotEmpty(alertTeamList)) {
            for (AlertTeamVo team : alertTeamList) {
                if (StringUtils.isNotBlank(team.getTeamEmail())) {
                    mailSet.add(team.getTeamEmail());
                }
            }
        }
    }

    private void addWorkerTeamUserMail(AlertVo alertVo, Set<String> mailSet) {
        List<AlertTeamVo> alertTeamList = alertVo.getTeamList();
        if (CollectionUtils.isEmpty(alertTeamList)) {
            alertTeamList = alertEventMapper.getAlertTeamByAlertId(alertVo.getId());
        }
        if (CollectionUtils.isNotEmpty(alertTeamList)) {
            for (AlertTeamVo team : alertTeamList) {
                List<UserVo> teamUserList = userMapper.getUserListByTeamUuid(team.getTeamUuid());
                if (CollectionUtils.isNotEmpty(teamUserList)) {
                    for (UserVo user : teamUserList) {
                        if (StringUtils.isNotBlank(user.getEmail())) {
                            mailSet.add(user.getEmail());
                        }
                    }
                }
            }
        }
    }

    private String makeupGroupName(AlertVo alertVo, JSONArray toUserList, JSONArray ccUserList, AlertMailReceiverVo receiverVo) {
        List<String> groupNameList = new ArrayList<>();
        addGroupName(alertVo, groupNameList, toUserList);
        addGroupName(alertVo, groupNameList, ccUserList);
        if (CollectionUtils.isNotEmpty(groupNameList)) {
            return String.join("、", groupNameList);
        }
        Set<String> toList = receiverVo.getToList();
        if (CollectionUtils.isNotEmpty(toList)) {
            return String.join(",", toList);
        }
        Set<String> ccList = receiverVo.getCcList();
        if (CollectionUtils.isNotEmpty(ccList)) {
            return String.join(",", ccList);
        }
        return "未知处理组";
    }

    private void addGroupName(AlertVo alertVo, List<String> groupNameList, JSONArray userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return;
        }
        for (int i = 0; i < userList.size(); i++) {
            String userUuid = userList.getString(i);
            if (("alertUserType#" + AlertUserType.WORKER_TEAM.getValue()).equals(userUuid)
                    || ("alertUserType#" + AlertUserType.WORKER_TEAM_USER.getValue()).equals(userUuid)) {
                List<AlertTeamVo> alertTeamList = alertVo.getTeamList();
                if (CollectionUtils.isEmpty(alertTeamList)) {
                    alertTeamList = alertEventMapper.getAlertTeamByAlertId(alertVo.getId());
                }
                if (CollectionUtils.isNotEmpty(alertTeamList)) {
                    for (AlertTeamVo team : alertTeamList) {
                        if (StringUtils.isNotBlank(team.getTeamName()) && !groupNameList.contains(team.getTeamName())) {
                            groupNameList.add(team.getTeamName());
                        }
                    }
                }
            } else if (userUuid.startsWith("team#")) {
                TeamVo teamVo = teamMapper.getTeamByUuid(AuthType.removePrefix(userUuid));
                if (teamVo != null && StringUtils.isNotBlank(teamVo.getName()) && !groupNameList.contains(teamVo.getName())) {
                    groupNameList.add(teamVo.getName());
                }
            }
        }
    }
}
