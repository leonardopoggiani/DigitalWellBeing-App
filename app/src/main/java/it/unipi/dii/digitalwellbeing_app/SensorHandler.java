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
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;



public class SensorHandler extends Service implements SensorEventListener {

    private final IBinder binder = new LocalBinder();

    //Class used for the client Binder
    public class LocalBinder extends Binder {
        SensorHandler getService() {
            return SensorHandler.this;
        }
    }

    private PowerManager.WakeLock wakeLock;

    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor proximity;
    private Sensor gyroscope;
    private Sensor gravity;
    private Sensor rotation;
    private Sensor linear;
    private Sensor magnetometer;

    private HandlerThread detectionThread;
    private Handler detectionHandler;
    //Timer used to start and stop the sampling with higher rate on the smartwatch
    private HandlerThread fastSamplingThread;
    private Handler fastSamplingHandler;

    private ServiceCallbacks serviceCallbacks;


    private static final String TAG = "SensorHandler";

    //Used to find out if the fast sampling is in progress
    private boolean started;
    private boolean goodProximity;
    private boolean goodAccel;
    private int counter;

    TreeMap<Long,Float[]> toBeClassified = new TreeMap<>();
    static boolean already_recognized = true;
    final float[] rotationMatrix = new float[9];
    final float[] orientationAngles = new float[3];
    Intent intentClassResult;

    private ActivityClassifier classifier = new ActivityClassifier(this);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "OnStartCommand SensorHandler");
        if (intent.getAction() != null && intent.getAction().compareTo("Command") == 0) {
            String command = intent.getStringExtra("command_key");
            switch (command) {
                case "START":
                    Log.d(TAG, "Start case");
                    counter = 0;
                    initializeSensorHandler();
                    //Start the sensorListener with a low sampling frequency and initialize the detection timer
                    if (startListener(Configuration.HIGH_SAMPLING_RATE)) {
                        //started = false;
                        //goodProximity = false;
                        //goodAccel = false;
                        //initializeDetectionTimer();
                        Log.d(TAG, "Detection Activated");
                    } else
                        Log.d(TAG, "Error in starting sensors listeners");
                    break;
                case "STOP":
                    Log.d(TAG, "SensorHandlerService Stopped");
                    //When FastSampling is active the related timer must be cancelled before to stop the service
                    /*if (started) {
                        wakeLock.release();
                        fastSamplingThread.quit();
                        fastSamplingThread = null;
                        fastSamplingHandler = null;

                    }*/
                    if (sm != null) {
                        stopListener();
                        /*if (detectionThread != null) {
                            detectionThread.quit();
                            detectionThread = null;
                            detectionHandler = null;
                        }*/

                    } else
                        Log.d(TAG, "SensorManager null");
                    stopSelf();
                    break;
                default:
                    Log.d(TAG, "Default Case");
                    break;
            }
            //} else {
            //Log.d(TAG, "SensorHandler activated");
            // }

        }
        else if(intent.getAction() != null && intent.getAction().compareTo("Classification_Result") == 0)
        {
            intentClassResult = intent;

            if(intent.getStringExtra("activity").equals("PICKUP")) {
                if(serviceCallbacks != null) {
                    serviceCallbacks.setActivityAndCounter("PICKUP!");
                }
            }
            else if(!intent.getStringExtra("activity").equals("OTHER")) {
                serviceCallbacks.setActivity("OTHER!");
            }


        }
        return Service.START_STICKY;
    }



    //Initialize the Detection Timer. When it will expire the sampling operations will be stopped
    /*private void initializeDetectionTimer() {
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
                //stopSelf();
    }*/
        //},Configuration.DETECTION_DELAY);


    //Initialize the Fast Sampling Timer. When it will expire the sampling rate will be decreased and

    private void initializeTimerFastSampling() {
        //wakeLock.acquire(Configuration.FAST_SAMPLING_DELAY);
        //fastSamplingThread = new HandlerThread("SensorHandler");
        //fastSamplingThread.start();
        //fastSamplingHandler = new Handler(fastSamplingThread.getLooper());
        //fastSamplingHandler.postDelayed(new Runnable() {
            //public void run() {
                //wakeLock.release();

                if(stopListener()) {
                    Log.d(TAG, "Stop listener");
                }
                else
                    Log.d(TAG, "Errors in storing collected data");
                startListener(SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Sampling rate decreased");
    }
        //},Configuration.FAST_SAMPLING_DELAY);

    private void setFastSampling() {

        if(stopListener()) {
            Log.d(TAG, "Stop listener");
        }
        else
            Log.d(TAG, "Errors in storing collected data");
        startListener(Configuration.HIGH_SAMPLING_RATE);
        started = true;
        Log.d(TAG, "Sampling rate increased");
    }

    private void setLowSampling() {

        if(stopListener()) {
            Log.d(TAG, "Stop listener");
        }
        else
            Log.d(TAG, "Errors in storing collected data");
        startListener(Configuration.LOW_SAMPLING_RATE);
        started = false;
        Log.d(TAG, "Sampling rate decreased");
    }


    protected Boolean startListener(int rate){

        //Se il rate è quello basso prelevo solo dall'accelerometro e il sonsore di prossimità
        if(rate == Configuration.LOW_SAMPLING_RATE){
            Log.d(TAG, "Delay normal activated");
            return (sm.registerListener(this, accelerometer, rate) && sm.registerListener (this, proximity, rate));

        }

        //Altrimenti, attivo tutti prelevo da tutti i sensori per classifirare un pickup
        if(rate == Configuration.HIGH_SAMPLING_RATE &&
                sm.registerListener(this, accelerometer, rate) &&
                sm.registerListener(this, rotation, rate) &&
                sm.registerListener(this, gyroscope, rate) &&
                sm.registerListener(this, gravity, rate) &&
                sm.registerListener(this, linear, rate) &&
                sm.registerListener (this, magnetometer, rate) &&
                sm.registerListener (this, proximity, rate)) {

        }
        /*else if(rate == SensorManager.SENSOR_DELAY_FASTEST){
            sm.registerListener (this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            sm.registerListener (this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
            sm.registerListener (this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
            sm.registerListener (this, rotation, SensorManager.SENSOR_DELAY_FASTEST);
            sm.registerListener (this, linear, SensorManager.SENSOR_DELAY_FASTEST);
            sm.registerListener (this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
            sm.registerListener (this, proximity, SensorManager.SENSOR_DELAY_FASTEST);*/


            started = true;
            //initializeTimerFastSampling();
            Log.d(TAG,"Fast Sampling activated");

        //}
        /*} else {
            //registerListener on some sensor could be failed so the rate must be reset on low frequency rate
            stopListener();
            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sm.registerListener (this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG,"Some registration is failed");
            return false;
        }*/
        return true;
    }

    private void initializeSensorHandler() {
        Log.d(TAG, "Initialize sensor handler");
        started = false;

        //Sets up the wakelock level to "PARTIAL_WAKE_LOCK" in order to mantain the cpu awake during
        //the fast sampling operations, so that each sensor's event is processed and no missing value occurs
        //PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                //"HandActivitySignal::WakelockTag");

        sm = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);

        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        gravity = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void addMapValues(SensorEvent event, int i1, int i2, int i3) {
        boolean ret = false;

        // puó succedere che arrivino due valori di accelerometro consecutivi, si potrebbe fare quindi la media anziché scartare il valore
        // la media sarebbe sempre tra due campioni non molto distanti tra loro, accettabile come approssimazione?

        for(int i = i1; i <= i3 ; i++){
            if(toBeClassified.size() != 0 && !isFull()) {

                if(Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] != null) {
                    Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] =
                            (Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] + event.values[i % 3]) / 2;
                } else {
                    Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] = event.values[i % 3];
                }

                ret = true;
            }
        }

        if(!ret) {
            toBeClassified.put(event.timestamp, new Float[18]);

            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i1] = event.values[0];
            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i2] = event.values[1];
            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i3] = event.values[2];
        }

        // si puó prendere un campione ogni 10 (non abbiamo bisogno di tanti campioni per classificare)
        // oppure si puó pensare di aggregare questi campioni in qualche modo (media?)
        if(toBeClassified.size() >= 50) {
            Collection<Float[]> values = toBeClassified.values();
            Float[] toClassify = new Float[18];
            int[] count = new int[18];

            for(int i = 0; i < 18; i++) {
                for (Float[] value : values) {
                    if (value[i] == null) {
                        continue;
                    } else {
                        count[i]++;
                    }

                    if (toClassify[i] == null) {
                        toClassify[i] = value[i];
                    } else {
                        toClassify[i] = (toClassify[i] + value[i]);
                    }
                }
            }

            for(int i = 0; i < 18; i++) {
                toClassify[i] = toClassify[i] / count[i];
            }

            Intent intentClassification = new Intent(this, ClassificationService.class);
            intentClassification.putExtra("sampleArray", toClassify);
            intentClassification.putExtra("treeMap", toBeClassified);
            intentClassification.setAction("Classify");
            Log.d(TAG, "Start Service");
            startService(intentClassification);
            toBeClassified.clear();
        }
    }

    private boolean isFull() {
        for(int i = 0; i < Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey())).length; i++) {
            if(Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] == null) {
                return false;
            }
        }
        return true;
    }


    //Called when detection period of 5 minutes is finished or when changing the sampling period
    protected Boolean stopListener(){
        if(sm != null)
            sm.unregisterListener(this);
        started = false;
        return true;
    }

    // TODO
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //TODO controllo nel caso in cui sia attivo il low sampling se sta per iniziare un'estrazione
        /*
        //Il fast sampling non è attivo
        if(!started){
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if(!goodAccel)
                {
                    goodAccel = checkRangePocket(event);

                }
            }
            else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                if(!goodProximity)
                {
                    if (event.values[0] == 0.0) {
                        goodProximity = true;
                    }

                }

            }
            if(goodProximity && goodAccel)
            {
                started = true;
                setFastSampling();
                return;
            }

        }
        */
        if(started) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                addMapValues(event, 0, 1, 2);
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                addMapValues(event, 3, 4, 5);
            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                addMapValues(event, 6, 7, 8);
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                addMapValues(event, 9, 10, 11);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                addMapValues(event, 12, 13, 14);
            } else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                event.values[0] = (float) Math.toDegrees(orientationAngles[0]);
                event.values[1] = (float) Math.toDegrees(orientationAngles[1]);
                event.values[2] = (float) Math.toDegrees(orientationAngles[2]);

                addMapValues(event, 15, 16, 17);
            } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                Log.d(TAG, "Proximity: " + event.values[0]);
                //TODO aggiungere controllo anche sui dati dell'accelerometro sia per settare already_recognized ma anche per il samplig fast
                if (event.values[0] == 0.0) {
                    already_recognized = false;
                }
            }
        }
    }


    public boolean checkRangePocket(SensorEvent event) {
        return (event.values[0] >= Configuration.X_LOWER_BOUND_POCKET && event.values[0] <= Configuration.X_UPPER_BOUND_POCKET) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_POCKET && event.values[1] <= Configuration.Y_UPPER_BOUND_POCKET) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_POCKET && event.values[2] <= Configuration.Z_UPPER_BOUND_POCKET);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO document why this method is empty
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

}