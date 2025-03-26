package com.rel.mujde;

import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

public class InjectionRequestHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int procId = intent.getIntExtra("proc_id", 0);
        String packageName = intent.getStringExtra("pkg_name");

        if (procId == 0 || packageName == null || packageName.isEmpty()) {
            return;
        }

        Intent serviceIntent = new Intent(context, FridaInjectorService.class);
        serviceIntent.putExtra("proc_id", procId);
        serviceIntent.putExtra("pkg_name", packageName);
        context.startService(serviceIntent);
    }
}
