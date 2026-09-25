package com.niftydirection.agent;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL="snapshot_reminders";
    @Override public void onReceive(Context c,Intent intent){
        NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)m.createNotificationChannel(new NotificationChannel(CHANNEL,"Snapshot reminders",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent pi=PendingIntent.getActivity(c,1,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        m.notify(1001,new android.app.Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle("NIFTY Direction Agent").setContentText("Upload the next option-chain screenshot for a fresh analysis.")
            .setContentIntent(pi).setAutoCancel(true).build());
    }
}
