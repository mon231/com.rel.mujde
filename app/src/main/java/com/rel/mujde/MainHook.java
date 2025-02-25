package com.rel.mujde;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String PREFS_NAME = "ToastConfigs";
    private static final String CONFIG_KEY = "configs";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || lpparam.packageName == null) return;

        // Log that we're loading the hook
        XposedBridge.log("[ToastConfig] Loading hook for package: " + lpparam.packageName);

        // Check if this is our own app
        if (lpparam.packageName.equals("com.rel.mujde")) {
            XposedBridge.log("[ToastConfig] Loading in our own app");
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param == null) return;

                    Context context = (Context) param.thisObject;
                    if (context == null) return;

                    XposedBridge.log("[ToastConfig] Hooked onCreate for: " + lpparam.packageName);

                    try {
                        Context moduleContext = context.createPackageContext(
                            "com.rel.mujde",
                            Context.CONTEXT_IGNORE_SECURITY
                        );

                        SharedPreferences prefs = moduleContext.getSharedPreferences(
                            PREFS_NAME,
                            Context.MODE_PRIVATE
                        );

                        // Get all configurations
                        String json = prefs.getString(CONFIG_KEY, "[]");
                        Type type = new TypeToken<List<ConfigAdapter.Config>>(){}.getType();
                        List<ConfigAdapter.Config> configs = new Gson().fromJson(json, type);

                        // Find matching configuration
                        if (configs != null) {
                            for (ConfigAdapter.Config config : configs) {
                                if (lpparam.packageName.equals(config.packageName)) {
                                    XposedBridge.log("[ToastConfig] Found config for: " + lpparam.packageName);
                                    Toast.makeText(context, config.message, Toast.LENGTH_LONG).show();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        XposedBridge.log("[ToastConfig] Error in hook: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            XposedBridge.log("[ToastConfig] Error setting up hook: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
