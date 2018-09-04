package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(value = { "changeshistory", "history", "actions" })
public class OnlyOfficeCallback {

    @JsonProperty(value = "changesurl")
    private String changesUrl = null;

    @JsonProperty(value = "forcesavetype")
    private int forceSaveType = 0;

    @JsonProperty(value = "key")
    private String key;

    @JsonProperty(value = "status")
    private int status;

    @JsonProperty(value = "url")
    private String url;

    @JsonProperty(value = "userdata")
    private String userData;

    @JsonProperty(value = "users")
    private List<String> users;

    @JsonProperty(value = "lastsave")
    private Date lastSave;

    @JsonProperty(value = "notmodified")
    private boolean notModified = true;

    public OnlyOfficeCallback() {
        super();
    }

    public String getChangesUrl() {
        return changesUrl;
    }

    public int getForceSaveType() {
        return forceSaveType;
    }

    public String getKey() {
        return key;
    }

    public int getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public String getUserData() {
        return userData;
    }

    public List<String> getUsers() {
        return users;
    }

    public Date getLastSave() {
        return lastSave;
    }

    public boolean isModified() {
        return notModified == false;
    }

    public boolean isNotModified() {
        return notModified;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changesUrl == null) ? 0 : changesUrl.hashCode());
        result = prime * result + forceSaveType;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((lastSave == null) ? 0 : lastSave.hashCode());
        result = prime * result + (notModified ? 1231 : 1237);
        result = prime * result + status;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((userData == null) ? 0 : userData.hashCode());
        result = prime * result + ((users == null) ? 0 : users.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OnlyOfficeCallback other = (OnlyOfficeCallback) obj;
        if (changesUrl == null) {
            if (other.changesUrl != null)
                return false;
        } else if (!changesUrl.equals(other.changesUrl))
            return false;
        if (forceSaveType != other.forceSaveType)
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (lastSave == null) {
            if (other.lastSave != null)
                return false;
        } else if (!lastSave.equals(other.lastSave))
            return false;
        if (notModified != other.notModified)
            return false;
        if (status != other.status)
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (userData == null) {
            if (other.userData != null)
                return false;
        } else if (!userData.equals(other.userData))
            return false;
        if (users == null) {
            if (other.users != null)
                return false;
        } else if (!users.equals(other.users))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "OnlyOfficeCallback [changesUrl=%s, forceSaveType=%s, key=%s, status=%s, url=%s, userData=%s, users=%s, lastSave=%s, notModified=%s]",
                changesUrl, forceSaveType, key, status, url, userData, users, lastSave, notModified);
    }

}
