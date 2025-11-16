package com.location.tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executors;

public class LocationTrackerService extends Service {
    static final String ACTION_BROADCAST = (
            LocationTrackerService.class + ".broadcast"
    );
    static final String ACTION_STOP_SERVICE = (
            LocationTrackerService.class + ".stopTracking"
    );
    public static final String LOCATION_TRACKING_CHANNEL = "location_tracking_channel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private static String BASE_URL = "";

    private static JSONObject CALL_DATA = new JSONObject();


    @Override
    public void onCreate() {

        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        startForeground(100, createNotification());
        LocationRequest locationRequest = new LocationRequest
                .Builder(120000)
                .setMaxUpdateDelayMillis(5000)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setWaitForAccurateLocation(true).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result.getLastLocation() != null) {
                    Log.d(LocationTrackerService.class.toString(), "Lat : " + result.getLastLocation().getLatitude() + " lon: " + result.getLastLocation().getLongitude());
                    //trigger broadcast receiver
                    Intent intent = new Intent(ACTION_BROADCAST);
                    intent.putExtra("location", result.getLastLocation());
                    getApplicationContext().sendBroadcast(intent);
                    //Post data to server
                    postLocationToServer(result.getLastLocation());
                }
            }
        };
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        Log.d(this.getClass().toString(), "onCreate");
    }


    public static Intent getCancelIntent(Context context){
        Intent stopIntent = new Intent(context, LocationTrackerService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        return stopIntent;
    }

    private Notification createNotification() {
        PendingIntent pendingIntent =  PendingIntent.getService(getApplicationContext(), 999, getCancelIntent(getApplicationContext()), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, LOCATION_TRACKING_CHANNEL)
                .setContentTitle("Location Tracking")
                .setContentText("Sending live location to server")
                .setSmallIcon(R.drawable.baseline_vehicle)
                .addAction(R.drawable.ic_stop, "Stop Tracking", pendingIntent) // Button in the notification
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = null;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(LOCATION_TRACKING_CHANNEL, "LocationTrackerService", NotificationManager.IMPORTANCE_HIGH);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_STOP_SERVICE.equals(action)) {
            Log.d(LocationTrackerService.class.toString(), "onStartCommand():");
            Intent bcIntent = new Intent(ACTION_STOP_SERVICE);
            getApplicationContext().sendBroadcast(bcIntent);
            stopSelf(); // triggers onDestroy()
            return START_NOT_STICKY;
        }
        BASE_URL = Objects.requireNonNull(Objects.requireNonNull(intent.getExtras()).get("BASE_URL")).toString();
        try {
                String dataString = Objects.requireNonNull(intent.getExtras().getString("DATA"));
                Log.d(LocationTrackerService.class.toString(),"Call Data: "+ dataString);
                CALL_DATA = new JSONObject(dataString);
        } catch (JSONException e) {
               Log.d(LocationTrackerService.class.toString(),"Call Data: not found");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(this.getClass().toString(), "onDestroy()");

        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not binding
    }


    private void postLocationToServer(Location location) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(BASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");


                if(CALL_DATA == null){
                    CALL_DATA = new JSONObject();
                }


                CALL_DATA.put("lat", String.format("%s", location.getLatitude()));
                CALL_DATA.put("lng", String.format("%s", location.getLongitude()));
                Log.d(LocationTrackerService.class.toString(),"CALL DATA: "+ CALL_DATA.toString());

                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                //String json = String.format("{\"lat\": \"%s\", \"lng\": \"%s\"}", location.getLatitude(), location.getLongitude());
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = CALL_DATA.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int responseCode = conn.getResponseCode();
                Log.d(this.getClass().toString(), " response url: " +  conn.getURL() +" response code: " + responseCode + " response message: " + conn.getResponseMessage() + "  req body: " + CALL_DATA.toString());
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}

