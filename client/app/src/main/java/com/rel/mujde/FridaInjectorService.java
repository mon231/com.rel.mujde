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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FridaInjectorService extends Service {
    private Thread injectorThread = null;
    private boolean shouldContinueLooping = false;
    private Notification serviceNotificaton = null;
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

        if (injectorThread != null && injectorThread.isAlive()) {
            return START_STICKY;
        }

        if (serviceNotificaton == null) {
            serviceNotificaton = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Mujde Injector Service")
                .setContentText("Mujde injector is running")
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .build();

            final int SERVICE_ID = 845;
            startForeground(SERVICE_ID, serviceNotificaton);
        }

        injectorThread = new Thread(() -> injectorThreadLogic());
        injectorThread.start();

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
