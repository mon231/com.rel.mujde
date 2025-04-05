package com.rel.mujde;

import java.io.File;
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import static android.content.Context.MODE_WORLD_READABLE;

public class InjectionRequestHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        InjectionRequest request = InjectionRequest.fromExtra(intent);

        if (request == null) {
            return;
        }

        ScriptUtils.getScriptsForPackage(
            request.getPackageName(),
            context.getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE)
        ).forEach(script -> {
            File scriptFile = ScriptUtils.getScriptFile(context, script);
            Log.d("[Mujde]", "about to frida-inject " + script + " into " + request.toString());
            final String INJECTOR_PATH = context.getApplicationInfo().nativeLibraryDir + "/libfrida-inject.so";

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "su", "-c",
                    INJECTOR_PATH, "-e",
                    "-p", String.valueOf(request.getPid()),
                    "-s", scriptFile.getAbsolutePath()
                );

                processBuilder.start();
            } catch (Exception e) {
                Log.d("[Mujde]", "Error during frida injection: " + e.getMessage());
            }
        });
    }
}
