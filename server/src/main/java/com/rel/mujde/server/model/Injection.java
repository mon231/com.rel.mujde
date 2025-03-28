package com.rel.mujde.server.model;

// TODO: rename to 'recommendetion' in db and in the code and server
public class Injection {
    private int appId;
    private int scriptId;
    private String packageName;
    private String scriptName;

    public Injection() { }

    public Injection(int appId, int scriptId) {
        this.appId = appId;
        this.scriptId = scriptId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public int getScriptId() {
        return scriptId;
    }

    public void setScriptId(int scriptId) {
        this.scriptId = scriptId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    @Override
    public String toString() {
        return "Injection{" +
            "appId=" + appId +
            ", scriptId=" + scriptId +
            ", packageName='" + packageName + '\'' +
            ", scriptName='" + scriptName + '\'' +
            '}';
    }
}
