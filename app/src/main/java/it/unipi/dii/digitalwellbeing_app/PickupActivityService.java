package it.unipi.dii.digitalwellbeing_app;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class PickupActivityService extends Service {
    private Intent intentClassResult;
    private ServiceCallbacks serviceCallbacks;
    public PickupActivityService() {
    }

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        if(intent.getAction() != null && intent.getAction().compareTo("Classification_Result") == 0)
        {
            intentClassResult = intent;

            if(intent.getStringExtra("activity").equals("PICKUP")) {
                //started = false;
                //goodProximity = false;
                //goodAccel = false;

                serviceCallbacks.setActivityAndCounter("PICKUP!");

            }
            else if(!intent.getStringExtra("activity").equals("OTHER")) {
                serviceCallbacks.setActivity("OTHER!");
            }


        }
        return Service.START_STICKY;
    }



    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}