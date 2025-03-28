package com.rel.mujde;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class InjectionRequestHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        InjectionRequest request = InjectionRequest.fromExtra(intent);

        if (request == null) {
            return;
        }

        Log.d("[Mujde]", "Received injection request" + request.toString());
        Intent serviceIntent = new Intent(context, FridaInjectorService.class);

        request.putExtra(serviceIntent);
        context.startService(serviceIntent);
    }
}
