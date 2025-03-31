package com.rel.mujde;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FeatureSpoofer implements IXposedHookLoadPackage {
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
            Process chmod = Runtime.getRuntime().exec("chmod 664 " + pref.getFile().getAbsolutePath());
            chmod.waitFor();
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

        // Hook Activity.onCreate to inject scripts
        hookActivityOnCreate(lpparam, scripts);
    }

    /**
     * Read the content of a script file
     * @param scriptName Name of the script file
     * @return List of lines in the script file
     */


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

    /**
     * Inject Frida scripts into target application
     * @param context Context to use for script injection
     * @param scriptNames List of script names to inject
     */
    private void injectFridaScripts(final Context context, final List<String> scriptNames) {
        // TODO: add to XSharedPreferences, sleep 3sec
    }

    private void hookActivityOnCreate(XC_LoadPackage.LoadPackageParam lpparam, final List<String> scripts) {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class.getName(),
                    lpparam.classLoader,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            final Activity activity = (Activity) param.thisObject;
                            final String packageName = activity.getPackageName();
                            final String appName = activity.getApplicationInfo().loadLabel(activity.getPackageManager()).toString();

                            // Show toast on the main thread
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String toastMessage = "Mujde: " + appName + " ("+packageName+") is running " + scripts.size() + " scripts";
                                        Toast toast = Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG);
                                        toast.setDuration(3000); // 3 seconds
                                        toast.show();
                                        log("Showed toast for " + packageName + " with " + scripts.size() + " scripts");

                                        // Inject Frida scripts
                                        injectFridaScripts(activity, scripts);
                                    } catch (Exception e) {
                                        log("Error showing toast: " + e.getMessage());
                                    }
                                }
                            }, 1000); // Delay by 1 second to ensure the activity is fully created
                        }
                    }
            );
        } catch (Throwable e) {
            log("Error hooking Activity.onCreate(): " + e.getMessage());
        }
    }
}
