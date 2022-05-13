package it.unipi.dii.digitalwellbeing_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

    private SensorHandler sensorHandlerService;
    private ClassificationService classificationService;
    private static String TAG = "DigitalWellBeing";
    boolean bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intentSensorHandler = new Intent(this, SensorHandler.class);
        bindService(intentSensorHandler, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    public void onStartCommand(View view){

        Button start_button = (Button) findViewById(R.id.start);

        if(start_button.getText().toString().equals("START")) {
            Log.d(TAG, "Start Smartwatch sensing");
            Intent startIntent = new Intent(this, SensorHandler.class);
            startIntent.setAction("Command");
            startIntent.putExtra("command_key", "START");
            startService(startIntent);
            start_button.setText("STOP");

        } else if(start_button.getText() == "STOP") {
            Log.d(TAG, "Stop sensing");
            Intent stopIntent = new Intent(this, SensorHandler.class);
            stopIntent.setAction("Command");
            stopIntent.putExtra("command_key", "STOP");
            startService(stopIntent);
            start_button.setText("START");
        }

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
            bound = true;
            sensorHandlerService.setCallbacks(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }

    };


    @Override
    public void setActivityAndCounter(String actvity) {
        new Handler(Looper.getMainLooper()).post(() -> {
            TextView tv = findViewById(R.id.activity);
            TextView tv2 = findViewById(R.id.counter);

            tv.setText(actvity);
            Log.d(TAG, actvity);

            if(actvity.equals("PICKUP!")) {
                CharSequence counter = tv2.getText();
                int count = Integer.parseInt(counter.toString());
                count += 1;
                tv2.setText(String.valueOf(count));
                Log.d(TAG, String.valueOf(count));
            }
        });
    }

    @Override
    public String getActivity() {
        TextView tv = findViewById(R.id.activity);
        return tv.getText().toString();
    }

}




