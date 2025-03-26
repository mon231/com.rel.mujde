package com.rel.mujde;

import android.os.Build;
import android.util.Log;
import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import androidx.annotation.Nullable;

public class FridaInjectorService extends Service {
    private Thread injectorThread = null;
    private boolean shouldContinueLooping = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldContinueLooping = true;

        if (injectorThread == null || !injectorThread.isAlive()) {
            injectorThread = new Thread(() -> injectorThreadLogic());
            injectorThread.start();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shouldContinueLooping = false;

        if (injectorThread != null) {
            try {
                injectorThread.wait();
            } catch (Exception e) {
                Log.e("[Mujde]", "Error waiting for thread: " + e.getMessage());
            }
        }
    }

    private void injectorThreadLogic() {
        while (shouldContinueLooping) {
            try {
                String nativeLibDir = getApplicationInfo().nativeLibraryDir;
                String fridaInjectorPath = nativeLibDir + "/frida-inject";

                // TODO: fetch pid from XSharedPreferences, inject scripts, attach to pid
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "su", "-c", fridaInjectorPath, "-n", "com.rel.mujde", "-e"
                );

                Process fridaProcess = processBuilder.start();
                int exitCode = fridaProcess.waitFor();
                Log.d("[Mujde]", "Frida injector exited with code: " + exitCode);
            } catch (Exception e) {
                Log.e("[Mujde]", "Error during frida injection: " + e.getMessage());
            }

            try {
                final long HALF_SECOND = 500;
                Thread.sleep(HALF_SECOND);
            } catch (InterruptedException e) {
                Log.e("[Mujde]", "Error sleeping thread: " + e.getMessage());
            }
        }
    }
}
