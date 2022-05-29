package it.unipi.dii.digitalwellbeing_app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.TreeMap;



public class SensorHandler extends Service implements SensorEventListener {

    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor proximity;
    private Sensor gyroscope;
    private Sensor gravity;
    private Sensor rotation;
    private Sensor linear;
    private Sensor magnetometer;
    private static final String TAG = "SensorHandler";
    //Used to find out if the fast sampling is in progress
    private boolean started = false;
    private boolean goodProximity = false;
    private boolean goodAccel = false;
    private int counter;
    private boolean startCheckPosition = true;
    private Thread fastRun;
    TreeMap<Long,Float[]> toBeClassified = new TreeMap<>();
    static boolean already_recognized = true;
    final float[] rotationMatrix = new float[9];
    final float[] orientationAngles = new float[3];

    private ActivityClassifier classifier = new ActivityClassifier(this);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent.getAction() != null && intent.getAction().compareTo("Command") == 0) {
            //Start the sensorListener with a low sampling frequency and initialize the detection timer
            Runnable fastToRun = () -> {

                String command = intent.getStringExtra("command_key");
                switch (command) {
                    case "START":
                        initializeSensorHandler();
                        counter = 0;
                        //Start the sensorListener with a low sampling frequency and initialize the detection timer
                        if (startListener(Configuration.LOW_SAMPLING_RATE)) {
                            started = false;
                        }
                        break;
                    case "STOP":
                        if (sm != null) {
                            stopListener();
                            stopSelf();
                        }
                        break;
                    default:
                        break;
                }
            };

            fastRun = new Thread(fastToRun);
            fastRun.start();
        }
        else if (intent.getAction() != null && intent.getAction().compareTo("samplingRate") == 0) {

            fastRun.interrupt();
            fastRun = null;
            Runnable slowToRun = () -> {
                initializeSensorHandler();
                setLowSampling();
            };
            fastRun = new Thread(slowToRun);
            fastRun.start();
        }

        return Service.START_STICKY;
    }

    private void setFastSampling() {
        startListener(Configuration.HIGH_SAMPLING_RATE);
        startCheckPosition = false;
        started = true;
    }

    private void setLowSampling() {
        if(startListener(Configuration.LOW_SAMPLING_RATE))
        {
            started = false;
            startCheckPosition = true;
        }
    }

    protected Boolean startListener(int rate){

        //Se il rate è quello basso prelevo solo dall'accelerometro e il sensore di prossimità
        if(rate == Configuration.LOW_SAMPLING_RATE){
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

            started = true;
        }
        else {
            //registerListener on some sensor could be failed so the rate must be reset on low frequency rate
            stopListener();
            started = false;
            startCheckPosition = true;
            sm.registerListener(this, accelerometer, Configuration.LOW_SAMPLING_RATE);
            sm.registerListener (this, proximity, Configuration.LOW_SAMPLING_RATE);
            return true;
        }
        return true;
    }

    private void initializeSensorHandler() {
        started = false;
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

        if(toBeClassified.size() >= Configuration.SAMPLING_WINDOW) {
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
            startService(intentClassification);
            startCheckPosition = true;
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(startCheckPosition) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                goodProximity = checkGoodInPocketValue(event);
            }
            if (goodProximity) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    goodAccel = checkGoodInPocketValue(event);
                }
            }

            if (goodProximity && goodAccel && !started) {
                started = true;
                setFastSampling();
                return;

            }
        }
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
                already_recognized = event.values[0] == 0.0;
                disableTouch(event);
            }
        } else {
            disableTouch(event);
        }
    }

    public boolean checkRangePocket(SensorEvent event) {
        return ((event.values[0] >= Configuration.X_LOWER_BOUND_DOWNWARDS_LEG && event.values[0] <= Configuration.X_UPPER_BOUND_DOWNWARDS_LEG) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_DOWNWARDS_LEG && event.values[1] <= Configuration.Y_UPPER_BOUND_DOWNWARDS_LEG) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_DOWNWARDS_LEG && event.values[2] <= Configuration.Z_UPPER_BOUND_DOWNWARDS_LEG)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_UPWARDS_LEG && event.values[0] <= Configuration.X_UPPER_BOUND_UPWARDS_LEG) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_UPWARDS_LEG && event.values[1] <= Configuration.Y_UPPER_BOUND_UPWARDS_LEG) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_UPWARDS_LEG && event.values[2] <= Configuration.Z_UPPER_BOUND_UPWARDS_LEG)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_UPWARDS_POCKET && event.values[0] <= Configuration.X_UPPER_BOUND_UPWARDS_POCKET) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_UPWARDS_POCKET && event.values[1] <= Configuration.Y_UPPER_BOUND_UPWARDS_POCKET) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_UPWARDS_POCKET && event.values[2] <= Configuration.Z_UPPER_BOUND_UPWARDS_POCKET)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_DOWNWARDS_POCKET && event.values[0] <= Configuration.X_UPPER_BOUND_DOWNWARDS_POCKET) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_DOWNWARDS_POCKET && event.values[1] <= Configuration.Y_UPPER_BOUND_DOWNWARDS_POCKET) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_DOWNWARDS_POCKET && event.values[2] <= Configuration.Z_UPPER_BOUND_DOWNWARDS_POCKET)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_DOWNWARDS_LEFT_LEG_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_DOWNWARDS_LEFT_LEG_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_DOWNWARDS_LEFT_LEG_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_DOWNWARDS_LEFT_LEG_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_DOWNWARDS_LEFT_LEG_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_DOWNWARDS_LEFT_LEG_SIT)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_DOWNWARDS_RIGHT_LEG_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_DOWNWARDS_RIGHT_LEG_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_DOWNWARDS_RIGHT_LEG_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_DOWNWARDS_RIGHT_LEG_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_DOWNWARDS_RIGHT_LEG_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_DOWNWARDS_RIGHT_LEG_SIT)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_DOWNWARDS_LEFT_POCKET_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_DOWNWARDS_LEFT_POCKET_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_DOWNWARDS_LEFT_POCKET_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_DOWNWARDS_LEFT_POCKET_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_DOWNWARDS_LEFT_POCKET_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_DOWNWARDS_LEFT_POCKET_SIT)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_DOWNWARDS_RIGHT_POCKET_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_DOWNWARDS_RIGHT_POCKET_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_DOWNWARDS_RIGHT_POCKET_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_DOWNWARDS_RIGHT_POCKET_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_DOWNWARDS_RIGHT_POCKET_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_DOWNWARDS_RIGHT_POCKET_SIT)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_UPWARDS_LEFT_LEG_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_UPWARDS_LEFT_LEG_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_UPWARDS_LEFT_LEG_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_UPWARDS_LEFT_LEG_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_UPWARDS_LEFT_LEG_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_UPWARDS_LEFT_LEG_SIT)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_UPWARDS_RIGHT_LEG_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_UPWARDS_RIGHT_LEG_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_UPWARDS_RIGHT_LEG_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_UPWARDS_RIGHT_LEG_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_UPWARDS_RIGHT_LEG_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_UPWARDS_RIGHT_LEG_SIT)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_UPWARDS_LEFT_POCKET_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_UPWARDS_LEFT_POCKET_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_UPWARDS_LEFT_POCKET_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_UPWARDS_LEFT_POCKET_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_UPWARDS_LEFT_POCKET_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_UPWARDS_LEFT_POCKET_SIT)) ||
                ((event.values[0] >= Configuration.X_LOWER_BOUND_UPWARDS_RIGHT_POCKET_SIT && event.values[0] <= Configuration.X_UPPER_BOUND_UPWARDS_RIGHT_POCKET_SIT) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_UPWARDS_RIGHT_POCKET_SIT && event.values[1] <= Configuration.Y_UPPER_BOUND_UPWARDS_RIGHT_POCKET_SIT) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_UPWARDS_RIGHT_POCKET_SIT && event.values[2] <= Configuration.Z_UPPER_BOUND_UPWARDS_RIGHT_POCKET_SIT));

    }

    public boolean checkGoodInPocketValue(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            return checkRangePocket(event);
        }
        else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            return event.values[0] == 0.0;
        }
        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void disableTouch(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            WindowManager.LayoutParams params = MainActivity.getInstance().getWindow().getAttributes();
            if(event.values[0] == 0.0) {
                params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                MainActivity.getInstance().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                params.screenBrightness = 0f;
            }
            else {
                MainActivity.getInstance().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                params.screenBrightness = -1f;
            }
            MainActivity.getInstance().getWindow().setAttributes(params);
        }
    }

}
