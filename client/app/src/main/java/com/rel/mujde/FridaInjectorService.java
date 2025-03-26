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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.content.Context.MODE_WORLD_READABLE;

public class FridaInjectorService extends Service {
    private Thread injectorThread = null;
    private SharedPreferences pref = null;
    private boolean shouldContinueLooping = false;
    private BlockingQueue<InjectionRequest> pendingRequests = new LinkedBlockingQueue<>();
    final private String CHANNEL_ID = "FrideInjectorChannel";
    private Notification serviceNotificaton = null;

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
            try {
                pendingRequests.put(request);
            } catch (InterruptedException e) {
                Log.e("[Mujde]", "Injection-request enqueue error: " + e.getMessage());
            }
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
                injectorThread.interrupt();
                injectorThread.wait();

                injectorThread = null;
                serviceNotificaton = null;
            } catch (Exception e) {
                Log.e("[Mujde]", "Error waiting for thread: " + e.getMessage());
            }
        }
    }

    private InjectionRequest fetchRequest()
    {
        try {
            return pendingRequests.take();
        } catch (InterruptedException e) {
            Log.e("[Mujde]", "Injection-request dequeue error: " + e.getMessage());
            return null;
        }
    }

    private void injectorThreadLogic() {
        String fridaInjectorPath = getApplicationInfo().nativeLibraryDir + "/libfrida-inject.so";

        while (shouldContinueLooping) {
            InjectionRequest request = fetchRequest();

            if (request == null) {
                continue;
            }

            ScriptUtils.getScriptsForPackage(request.getPackageName(), pref).forEach(script -> {
                Log.d("[Mujde]", "about to frida-inject " + script + " into " + request.toString());
                String scriptFullPath = getApplicationContext().getFilesDir().getAbsolutePath() + "/scripts/" + script;

                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(
                        "su", "-c",
                        fridaInjectorPath, "-e",
                        "-p", String.valueOf(request.getPid()),
                        "-s", scriptFullPath
                    );

                    processBuilder.start();
                } catch (Exception e) {
                    Log.d("[Mujde]", "Error during frida injection: " + e.getMessage());
                }
            });
        }
    }
}
