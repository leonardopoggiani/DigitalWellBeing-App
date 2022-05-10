package it.unipi.dii.digitalwellbeing_app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;

public class SensorHandler extends Service implements SensorEventListener {
    private final IBinder binder = new LocalBinder();
    private ServiceCallbacks serviceCallbacks;

    //Class used for the client Binder
    public class LocalBinder extends Binder {
        SensorHandler getService() {
            return SensorHandler.this;
        }
    }
    private PowerManager.WakeLock wakeLock;

    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor rotation;
    private Sensor gravity;
    private Sensor linear;

    private File storagePath;
    private File accel;
    private File gyr;
    private File rot;
    private File grav;
    private File linearAcc;

    private FileWriter writerAcc;
    private FileWriter writerGyr;
    private FileWriter writerRot;
    private FileWriter writerGrav;
    private FileWriter writerLin;



    private HandlerThread detectionThread;
    private Handler detectionHandler;
    //Timer used to start and stop the sampling with higher rate on the smartwatch
    private HandlerThread fastSamplingThread;
    private Handler fastSamplingHandler;

    private static final String TAG = "SensorHandler";

    //Used to find out if the fast sampling is in progress
    private boolean started;
    private int counter;
    public SensorHandler() {
    }

    private void initializeSensorHandler() {
        Log.d(TAG, "Initialize sensor handler");
        started = false;

        //Sets up the wakelock level to "PARTIAL_WAKE_LOCK" in order to mantain the cpu awake during
        //the fast sampling operations, so that each sensor's event is processed and no missing value occurs
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "HandActivitySignal::WakelockTag");

        sm = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);

        gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotation = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        gravity = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        storagePath = getApplicationContext().getExternalFilesDir(null);
        Log.d(TAG, "[STORAGE_PATH]: "+storagePath);
    }


    //TODO
    //Da ricontrollare i range per il telefono in tasca, in su e in giù, schermo verso l'interno e schermo verso l'esterno
    //anche da seduti
    //Check if accelerometer axis data are in the range of values related to the phone inside the pocket
    /*
    public boolean isInRange(SensorEvent event) {
        if((event.values[0] >= Configuration.X_LOWER_BOUND_POCKET && event.values[0] <= Configuration.X_UPPER_BOUND_POCKET) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_POCKET && event.values[1] <= Configuration.Y_UPPER_BOUND_POCKET) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_POCKET && event.values[2] <= Configuration.Z_UPPER_BOUND_POCKET)) {
            Log.d(TAG, "ACC_X: "+event.values[0]+", ACC_Y: "+event.values[1]+", ACC_Z: "+event.values[2]+", TIMESTAMP: "+event.timestamp);
            return true;
        }
        else  return false;
    }*/

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

}