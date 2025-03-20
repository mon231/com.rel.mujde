package com.rel.mujde.server.model;

import java.time.LocalDateTime;

/**
 * Represents a Frida script.
 */
public class Script {
    private int scriptId;
    private String scriptName;
    private String scriptPath;
    private String networkPath;
    private LocalDateTime lastModified;
    private String content;  // Transient field, not stored in database

    public Script() {
        // Default constructor for JAX-RS
    }

    public Script(int scriptId, String scriptName, String scriptPath, String networkPath, LocalDateTime lastModified) {
        this.scriptId = scriptId;
        this.scriptName = scriptName;
        this.scriptPath = scriptPath;
        this.networkPath = networkPath;
        this.lastModified = lastModified;
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

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
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
                '}';
    }
}