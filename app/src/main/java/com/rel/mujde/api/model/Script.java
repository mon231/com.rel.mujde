package com.rel.mujde.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a Frida script from the server.
 */
public class Script {
    private int scriptId;
    private String scriptName;
    private String scriptPath;
    private String networkPath;
    private String lastModified;  // ISO-8601 formatted date string
    private String content;  // Transient field, not stored in database

    public Script() {
        // Default constructor for Gson
    }

    public int getScriptId() {
        return scriptId;
    }

    public void setScriptId(int scriptId) {
        this.scriptId = scriptId;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getNetworkPath() {
        return networkPath;
    }

    public void setNetworkPath(String networkPath) {
        this.networkPath = networkPath;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "Script{" +
                "scriptId=" + scriptId +
                ", scriptName='" + scriptName + '\'' +
                ", scriptPath='" + scriptPath + '\'' +
                ", networkPath='" + networkPath + '\'' +
                ", lastModified=" + lastModified +
                "}";
    }
}
