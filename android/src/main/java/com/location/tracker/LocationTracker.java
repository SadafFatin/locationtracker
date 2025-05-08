package com.location.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class LocationTracker {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }

    public Object trackLocation(){
        return "Location result";
    }

//    private Notification createNotification() {
//        String channelId = "com.location.tracker_location_channel";
//        NotificationChannel channel = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            channel = new NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW);
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
//        }
//
////        Intent stopIntent = new Intent(this, LocationTrackerService.class);
////        stopIntent.setAction("STOP_SERVICE");
////        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        return new NotificationCompat.Builder(this, channelId)
//                .setContentTitle("Tracking location...")
//                .setSmallIcon(R.drawable.ic_location)
//                //.addAction(R.drawable.ic_stop, "Stop", stopPendingIntent) // Button in the notification
//                //.setOngoing(true)
//
//                .build();
//    }
}
