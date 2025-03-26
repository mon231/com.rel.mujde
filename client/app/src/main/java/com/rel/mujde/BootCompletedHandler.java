package com.rel.mujde;

import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

public class BootCompletedHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, FridaInjectorService.class);
            context.startService(serviceIntent);

            Log.d("[Mujde]", "Device booted! Starting injector service!");
        }
    }
}
