package com.rel.mujde;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        Log.d("[Mujde]", "Device booted! Starting injector service!");
        Intent serviceIntent = new Intent(context, FridaInjectorService.class);
        context.startService(serviceIntent);
    }
}
