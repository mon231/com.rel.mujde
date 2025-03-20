package com.rel.mujde.server.model;

public class App {
    private int appId;
    private String packageName;

    public App() { }

    public App(int appId, String packageName) {
        this.appId = appId;
        this.packageName = packageName;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return "App{" +
                " appId=" + appId +
                ", packageName='" + packageName + "'" +
                '}';
    }
}
