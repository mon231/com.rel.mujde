package com.rel.mujde.server.model;

public class Recommendation {
    private int appId;
    private int scriptId;
    private String packageName;
    private String scriptName;

    public Recommendation() { }

    public Recommendation(int appId, int scriptId) {
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
        return "Recommendation{" +
            "appId=" + appId +
            ", scriptId=" + scriptId +
            ", packageName='" + packageName + '\'' +
            ", scriptName='" + scriptName + '\'' +
            '}';
    }
}
