package it.unipi.dii.digitalwellbeing_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

    private SensorHandler sensorHandlerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intentSensorHandler = new Intent(this, SensorHandler.class);
        bindService(intentSensorHandler, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // cast the IBinder and get SensorHandler Service instance
            SensorHandler.LocalBinder binder = (SensorHandler.LocalBinder) service;
            sensorHandlerService = binder.getService();
            //bound = true;
            sensorHandlerService.setCallbacks(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //bound = false;
        }
    };


}