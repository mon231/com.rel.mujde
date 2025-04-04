package com.rel.mujde;

import android.content.Intent;
import androidx.annotation.Nullable;

public class InjectionRequest {
    private int pid;
    private String packageName;

    public InjectionRequest(int pid, String packageName) {
        this.pid = pid;
        this.packageName = packageName;
    }

    public int getPid() {
        return pid;
    }

    public String getPackageName() {
        return packageName;
    }

    public void putExtra(Intent intent) {
        intent.putExtra("proc_id", pid);
        intent.putExtra("pkg_name", packageName);
    }

    @Nullable
    public static InjectionRequest fromExtra(Intent intent) {
        if (intent == null) {
            return null;
        }

        int pid = intent.getIntExtra("proc_id", 0);
        String packageName = intent.getStringExtra("pkg_name");

        if (pid == 0 || packageName == null || packageName.isEmpty()) {
            return null;
        }

        return new InjectionRequest(pid, packageName);
    }

    @Override
    public String toString() {
        return "InjectionRequest{pid=" + pid + ", packageName=" + packageName + "}";
    }
}
