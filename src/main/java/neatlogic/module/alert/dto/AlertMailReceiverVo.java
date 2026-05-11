package neatlogic.module.alert.dto;

import java.util.Set;

public class AlertMailReceiverVo {
    private Set<String> toList;
    private Set<String> ccList;
    private String groupName;

    public Set<String> getToList() {
        return toList;
    }

    public void setToList(Set<String> toList) {
        this.toList = toList;
    }

    public Set<String> getCcList() {
        return ccList;
    }

    public void setCcList(Set<String> ccList) {
        this.ccList = ccList;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
