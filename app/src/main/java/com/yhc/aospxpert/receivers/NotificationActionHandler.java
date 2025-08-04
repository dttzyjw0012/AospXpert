package com.yhc.aospxpert.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yhc.aospxpert.BuildConfig;
import com.yhc.aospxpert.utils.PXPreferences;

public class NotificationActionHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int versionToIgnore = intent.getIntExtra("updateVersionIgnored", BuildConfig.VERSION_CODE);

            PXPreferences.putInt("updateVersionIgnored", versionToIgnore);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int notificationId = intent.getIntExtra("notificationId", -1);
            if (notificationId != -1) {
                notificationManager.cancel(notificationId);
            }

            Log.d("NotificationActionHandler", "Update version ignored: " + versionToIgnore);
        }
    }
}
