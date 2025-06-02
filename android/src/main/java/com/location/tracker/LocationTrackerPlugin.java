package com.location.tracker;

import static com.location.tracker.LocationTrackerService.getCancelIntent;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;


@CapacitorPlugin(

        name = "LocationTrackerPlugin",
        permissions = {
                @Permission(
                        alias = "location",
                        strings = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, }

                )
        }


)
public class LocationTrackerPlugin extends Plugin {

    private static final int REQUEST_LOCATION = 1000;
    public static final String BASE_URL = "BASE_URL";
    public static final String DATA = "DATA";

    public static final String BASE_URL_KEY = "baseUrl";
    public static final String CALL_DATA_KEY = "data";
    private final LocationTracker implementation = new LocationTracker();
    private final LocationTrackerService locationTrackerService = null;

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }


    @PluginMethod(returnType=PluginMethod.RETURN_CALLBACK)
    public void trackLocation(final PluginCall call) {
        Log.d(this.getClass().toString(), "Base URL(plugin): "+call.getString("baseUrl"));
        Log.d(this.getClass().toString(), "CBID:"+call.getCallbackId());
        if(isLocationEnabled(bridge.getContext())){
            call.setKeepAlive(true);
            requestPermissionForAlias("location", call, "handlePermissionResult");
        }
        else{
            call.reject("Your Location Service is not Enabled");
        }
    }


    @PluginMethod
    public void stopTrackingLocation(PluginCall call) {
        bridge.getContext().stopService(getCancelIntent(bridge.getContext()));
        call.reject("result", "Location Tracking has been turned off");
    }



    @PermissionCallback
    public void handlePermissionResult (PluginCall call){
        if (hasPermission()) {
            // We got the permission!
            Log.d(this.getClass().toString(), "CBID:"+call.getCallbackId());
            call.setKeepAlive(true);
            startLocationService(call);

        } else {
            call.reject("Permission is required to take a picture");
            call.setKeepAlive(false);
        }
    }

    private void startLocationService(PluginCall call) {
        BroadcastReceiver locationBroadcaster = new LocationBroadcaster(call);
        IntentFilter filter = new IntentFilter(LocationTrackerService.ACTION_BROADCAST);
        IntentFilter filterStopTracking = new IntentFilter(LocationTrackerService.ACTION_STOP_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bridge.getActivity().registerReceiver(locationBroadcaster, filter, Context.RECEIVER_NOT_EXPORTED);
            bridge.getActivity().registerReceiver(locationBroadcaster, filterStopTracking, Context.RECEIVER_NOT_EXPORTED);
        }
        else{
            bridge.getActivity().registerReceiver(locationBroadcaster, filterStopTracking);
            bridge.getActivity().registerReceiver(locationBroadcaster, filterStopTracking);
        }

        Log.d(this.getClass().toString(),  "Registered Broadcast Listener with CBID: "+call.getCallbackId());


        Intent intent = new Intent(bridge.getContext(),LocationTrackerService.class);
        intent.putExtra(BASE_URL, call.getString(BASE_URL_KEY));
        intent.putExtra(DATA,  call.getObject(CALL_DATA_KEY).toString() );


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bridge.getActivity().startForegroundService(intent);
        }
        else {
            bridge.getActivity().startService(intent);
        }
        Log.d(this.getClass().toString(), "CBID:"+call.getCallbackId());

    }

    private boolean hasPermission(){
            return getPermissionState("location") == PermissionState.GRANTED;
    }



    private void fetchLastLocation(PluginCall call) {
        try {
            LocationServices.getFusedLocationProviderClient(
                    getContext()
            ).getLastLocation().addOnSuccessListener(
                    getActivity(),
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                call.resolve(formatLocation(location));
                            }
                        }
                    }
            );
        } catch (SecurityException ignore) {}
    }



    // Checks if device-wide location services are disabled
    private static Boolean isLocationEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm != null && lm.isLocationEnabled();
        } else {
            return  (
                    Settings.Secure.getInt(
                            context.getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            Settings.Secure.LOCATION_MODE_OFF
                    ) != Settings.Secure.LOCATION_MODE_OFF
            );

        }
    }



    public static JSObject formatLocation(Location location) {
        JSObject obj = new JSObject();
        obj.put("lat", location.getLatitude());
        obj.put("lon", location.getLongitude());
        // The docs state that all Location objects have an accuracy, but then why is there a
        // hasAccuracy method? Better safe than sorry.
        obj.put("accuracy", location.hasAccuracy() ? location.getAccuracy() : JSONObject.NULL);
        obj.put("altitude", location.hasAltitude() ? location.getAltitude() : JSONObject.NULL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (location.hasVerticalAccuracy()) {
                obj.put("altitudeAccuracy", location.getVerticalAccuracyMeters());
            } else {
                obj.put("altitudeAccuracy", JSONObject.NULL);
            }
        }
        // In addition to mocking locations in development, Android allows the
        // installation of apps which have the power to simulate location
        // readings in other apps.
        obj.put("simulated", location.isFromMockProvider());
        obj.put("speed", location.hasSpeed() ? location.getSpeed() : JSONObject.NULL);
        obj.put("bearing", location.hasBearing() ? location.getBearing() : JSONObject.NULL);
        obj.put("time", location.getTime());

        return obj;
    }



}
