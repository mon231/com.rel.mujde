package com.rel.mujde;

import android.os.Build;
import android.util.Log;
import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import java.util.concurrent.ConcurrentLinkedQueue;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static android.content.Context.MODE_WORLD_READABLE;

public class FridaInjectorService extends Service {
    private Thread injectorThread = null;
    private SharedPreferences pref = null;
    private boolean shouldContinueLooping = false;
    private Notification serviceNotificaton = null;
    private ConcurrentLinkedQueue<InjectionRequest> pendingRequests = new ConcurrentLinkedQueue<>();
    final private String CHANNEL_ID = "FrideInjectorChannel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // foreground services must create notification-channel (for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldContinueLooping = true;

        InjectionRequest request = InjectionRequest.fromExtra(intent);
        if (request != null) {
            pendingRequests.add(request);
        }

        if (pref == null) {
            pref = getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE);
        }

        if (serviceNotificaton == null) {
            serviceNotificaton = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Mujde Injector Service")
                .setContentText("Mujde injector is running")
                .build();

            final int SERVICE_ID = 845;
            startForeground(SERVICE_ID, serviceNotificaton);
        }

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

                injectorThread = null;
                serviceNotificaton = null;
            } catch (Exception e) {
                Log.e("[Mujde]", "Error waiting for thread: " + e.getMessage());
            }
        }
    }

    private void injectorThreadLogic() {
        String fridaInjectorPath = getApplicationInfo().nativeLibraryDir + "/libfrida-inject.so";

        while (shouldContinueLooping) {
            try {
                final long HALF_SECOND = 500;
                Thread.sleep(HALF_SECOND);
            } catch (InterruptedException e) {
                continue;
            }

            InjectionRequest request = pendingRequests.poll();

            if (request == null) {
                continue;
            }

            try {
                // TODO: fetch pid from XSharedPreferences, inject scripts, attach to pid
                ProcessBuilder processBuilder = new ProcessBuilder(
                    // "su", "-c", fridaInjectorPath, "-n", "com.rel.mujde", "-e"
                    "su", "-c", fridaInjectorPath, "-h"
                );

                Process fridaProcess = processBuilder.start();
                int exitCode = fridaProcess.waitFor();

                Log.d("[Mujde]", "Frida injector exited with code: " + exitCode);
            } catch (Exception e) {
                Log.e("[Mujde]", "Error during frida injection: " + e.getMessage());
            }
        }
    }
}
