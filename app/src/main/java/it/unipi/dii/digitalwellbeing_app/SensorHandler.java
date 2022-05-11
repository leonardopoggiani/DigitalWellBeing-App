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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "OnStartCommand SensorHandler");
        if(intent.getAction() != null && intent.getAction().compareTo("Command") == 0) {
            String command = intent.getStringExtra("command_key");
            switch(command) {
                case "START":
                    Log.d(TAG, "Start case");
                    counter = 0;
                    initializeSensorHandler();
                    //Start the sensorListener with a low sampling frequency and initialize the detection timer
                    if(startListener(SensorManager.SENSOR_DELAY_NORMAL)) {
                        //initializeDetectionTimer();
                        Log.d(TAG, "Detection Activated");
                    }
                    else
                        Log.d(TAG,"Error in starting sensors listeners");
                    break;
                case "STOP":
                    Log.d(TAG, "SensorHandlerService Stopped");
                    //When FastSampling is active the related timer must be cancelled before to stop the service
                    if(started) {
                        wakeLock.release();
                        fastSamplingThread.quit();
                        fastSamplingThread = null;
                        fastSamplingHandler = null;

                    }
                    if(sm != null) {
                        stopListener();
                        if(detectionThread != null) {
                            detectionThread.quit();
                            detectionThread = null;
                            detectionHandler = null;
                        }

                    }
                    else
                        Log.d(TAG, "SensorManager null");
                    stopSelf();
                    break;
                default:
                    Log.d(TAG, "Default Case");
                    break;
            }
        } else {
            Log.d(TAG, "SensorHandler activated");
        }
        return Service.START_STICKY;
    }

    /*//Initialize the Detection Timer. When it will expire the sampling operations will be stopped
    private void initializeDetectionTimer() {
        Log.d(TAG, "Timer "+Configuration.DETECTION_DELAY/60000+" minutes started");
        detectionThread = new HandlerThread("SensorHandler");
        detectionThread.start();
        detectionHandler = new Handler(detectionThread.getLooper());
        detectionHandler.postDelayed(new Runnable() {
            public void run() {
                Log.d(TAG, "run del thread");
                if(started) {
                    wakeLock.release();
                    Log.d(TAG, "wakeLock released");
                    fastSamplingThread.quit();
                    fastSamplingThread = null;
                    fastSamplingHandler = null;
                }
                if(stopListener())
                    Log.d(TAG, "Detection stopped");
                stopSelf();
            }
        },Configuration.DETECTION_DELAY);

    }

    //Initialize the Fast Sampling Timer. When it will expire the sampling rate will be decreased and
    //an Intent will be sent to the WearActitvitySerivce in order to notify that new data are ready to be sent
    private void initializeTimerFastSampling() {
        wakeLock.acquire(Configuration.FAST_SAMPLING_DELAY);
        fastSamplingThread = new HandlerThread("SensorHandler");
        fastSamplingThread.start();
        fastSamplingHandler = new Handler(fastSamplingThread.getLooper());
        fastSamplingHandler.postDelayed(new Runnable() {
            public void run() {
                wakeLock.release();
                //Sends to paired smartphone collected data and decrease the sampling rate
                if(stopListener()) {
                    Log.d(TAG, "Timer expired, sending files");
                }
                else
                    Log.d(TAG, "Errors in storing collected data");
                startListener(SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Sampling rate decreased");
            }
        },Configuration.FAST_SAMPLING_DELAY);
    }*/


    protected Boolean startListener(int rate){

        //Se il rate è quello basso prelevo solo dall'accelerometro e il sonsore di prossimità
        if(rate == SensorManager.SENSOR_DELAY_NORMAL){
            Log.d(TAG, "Delay normal activated");
            return sm.registerListener(this, accelerometer, rate);
        }

        //Altrimenti, attivo tutti prelevo da tutti i sensori per classifirare un pickup
        if(rate == SensorManager.SENSOR_DELAY_GAME &&
                sm.registerListener(this, accelerometer, rate) &&
                sm.registerListener(this, rotation, rate) &&
                sm.registerListener(this, gyroscope, rate) &&
                sm.registerListener(this, gravity, rate) &&
                sm.registerListener(this, linear, rate) ) {


            started = true;
            //initializeTimerFastSampling();
            Log.d(TAG,"Fast Sampling activated");
            return true;
        } else {
            //registerListener on some sensor could be failed so the rate must be reset on low frequency rate
            stopListener();
            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG,"Some registration is failed");
            return false;
        }

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

    }


    //Called when detection period of 5 minutes is finished or when changing the sampling period
    protected Boolean stopListener(){
        if(sm != null)
            sm.unregisterListener(this);
        started = false;
        return true;
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