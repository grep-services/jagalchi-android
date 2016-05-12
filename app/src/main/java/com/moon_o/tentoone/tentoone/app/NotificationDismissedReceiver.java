package com.moon_o.tentoone.tentoone.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by mucha on 16. 5. 12.
 */
public class NotificationDismissedReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getExtras().getInt("com.my.app.notificationid");
        Log.e("RECEIVER", notificationId+"");
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }
}
