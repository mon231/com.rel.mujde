package com.rel.mujde;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import android.os.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class InjectionRequester implements IXposedHookLoadPackage {
    private XSharedPreferences pref;

    private void log(String message) {
        XposedBridge.log("[Mujde] " + message);
    }

    private XSharedPreferences getPreferences() {
        if (pref == null) {
            pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, Constants.SHARED_PREF_FILE_NAME);
            pref.reload();

            if (!pref.getFile().canRead()) {
                log("ERROR: Preference file is not readable!");
                makeWorldReadable();
            }
        } else {
            pref.reload();
        }

        return pref;
    }

    private void makeWorldReadable() {
        try {
            Runtime.getRuntime()
            .exec("chmod 777 " + pref.getFile().getAbsolutePath())
            .waitFor();

            pref.reload();
        } catch (Exception e) {
            log("Failed to make preferences readable: " + e.getMessage());
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }

        String packageName = lpparam.packageName;
        if (packageName.equals(BuildConfig.APPLICATION_ID)) {
            return;
        }

        XSharedPreferences prefs = getPreferences();
        List<String> scripts = ScriptUtils.getScriptsForPackage(packageName, prefs);

        if (scripts == null || scripts.isEmpty()) {
            return;
        }

        hookActivityOnCreate(lpparam, scripts);
    }

    // TODO: ensure no scripts are stored in sharedpref, all in dedicatedfolder

    // TODO: move this code to service

    // private List<String> readScriptContent(String scriptName) {
    //     List<String> lines = new ArrayList<>();

    //     log("Reading script: " + scriptName);

    //     try {
    //         // Look for the script in the app's files directory
    //         File scriptsDir = new File(new File("/data/data/" + BuildConfig.APPLICATION_ID), "files/scripts");
    //         File scriptFile = new File(scriptsDir, scriptName);

    //         if (scriptFile.exists() && scriptFile.canRead()) {
    //             try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
    //                 String line;
    //                 while ((line = reader.readLine()) != null) {
    //                     lines.add(line);
    //                 }
    //                 log("Successfully read script: " + scriptName + " with " + lines.size() + " lines");
    //             }
    //         } else {
    //             log("Script file does not exist or is not readable: " + scriptFile.getAbsolutePath());

    //             // Try using XSharedPreferences as a fallback
    //             XSharedPreferences scriptPref = new XSharedPreferences(BuildConfig.APPLICATION_ID, "script_contents");
    //             String scriptContent = scriptPref.getString(scriptName, "");

    //             if (!scriptContent.isEmpty()) {
    //                 String[] lineArray = scriptContent.split("\n");
    //                 for (String line : lineArray) {
    //                     lines.add(line);
    //                 }
    //                 log("Read " + lines.size() + " lines from XSharedPreferences");
    //             }
    //         }
    //     } catch (Exception e) {
    //         log("Error reading script: " + e.getMessage());
    //     }

    //     // Add a fallback message if the script couldn't be read
    //     if (lines.isEmpty()) {
    //         lines.add("// Unable to read script: " + scriptName);
    //         lines.add("// Please place your script in: /data/data/" + BuildConfig.APPLICATION_ID + "/files/scripts/" + scriptName);
    //         lines.add("// You can use 'adb push your_script.js /data/data/" + BuildConfig.APPLICATION_ID + "/files/scripts/" + scriptName + "'");
    //     }

    //     return lines;
    // }

    private void hookActivityOnCreate(XC_LoadPackage.LoadPackageParam lpparam, final List<String> scripts) {
        XposedHelpers.findAndHookMethod(
            Activity.class.getName(),
            lpparam.classLoader,
            "onCreate",
            Bundle.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    final Activity activity = (Activity)param.thisObject;
                    final String packageName = activity.getPackageName();

                    // TODO: do we want delay?
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName("com.rel.mujde", "com.rel.mujde.InjectionRequestHandler"));

                                intent.putExtra("proc_id", Process.myPid());
                                intent.putExtra("pkg_name", packageName);

                                activity.sendBroadcast(intent);
                            } catch (Exception e) {
                                Log.d("[Mujde]", "Error showing toast: " + e.getMessage());
                            }
                        }
                    }, 1000); // Delay by 1 second to ensure the activity is fully created
                }
            }
        );
    }
}
