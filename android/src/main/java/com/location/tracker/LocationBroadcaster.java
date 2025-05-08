package com.location.tracker;

import static com.location.tracker.LocationTrackerPlugin.formatLocation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import java.util.Objects;

public class LocationBroadcaster extends BroadcastReceiver {
    PluginCall call;
    public LocationBroadcaster(PluginCall call) {
        this.call = call;
    }
    public LocationBroadcaster(){
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LocationBroadcaster.class.toString(), "onReceive()");
        Log.d(this.getClass().toString(), "CBID:"+ this.call.getCallbackId());
        Log.d(this.getClass().toString(), "action:"+ intent.getAction());
        if (call == null) {
            Log.d(this.getClass().toString(), "No call data");
            return;
        }
        if(Objects.equals(intent.getAction(), LocationTrackerService.ACTION_STOP_SERVICE)){
            Log.d(this.getClass().toString(), "Received Stop Command");

            JSObject obj = new JSObject();
            obj.put("result", "Location Tracking has been turned off");
            call.resolve(obj);
            context.unregisterReceiver(this);

        }
        else if(Objects.equals(intent.getAction(), LocationTrackerService.ACTION_BROADCAST)){
            Location location = intent.getParcelableExtra("location");
            assert location != null;
            Log.d(LocationBroadcaster.class.toString(), "Location Received: " + formatLocation(location));
            call.resolve(formatLocation(location));
        }

    }
}
