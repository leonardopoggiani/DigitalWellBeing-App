package it.unipi.dii.digitalwellbeing_app;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.TreeMap;

public class ClassificationService extends Service {

    private final IBinder binder = new LocalBinder();
    private ServiceCallbacks serviceCallbacks;
    private String TAG = "ClassificationService";
    private ActivityClassifier classifier;

    Intent intentResult;
    Intent intentData;

    public class LocalBinder extends Binder {
        ClassificationService getService() {
            return ClassificationService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");

        classifier = new ActivityClassifier(this);
        intentResult = new Intent(this, SensorHandler.class);
        intentResult.setAction("Classification_Result");
        intentData = intent;
        handleClassification();
        return Service.START_STICKY;
    }


    private void handleClassification() {

            Log.d(TAG, "Handle Classification!");
            Float[] sampleArray = (Float[])intentData.getExtras().get("sampleArray");
            TreeMap<Long,Float[]> toBeClassified;
            toBeClassified = new TreeMap<>((HashMap<Long,Float[]>)intentData.getExtras().get("treeMap"));
            boolean activity = classifier.classifySamples(sampleArray, toBeClassified);

            if(activity) {
                Log.d(TAG, "PICKUP");
                intentResult.putExtra("activity","PICKUP");

            }
            else if(!activity) {
                Log.d(TAG,"OTHERS");
                intentResult.putExtra("activity","OTHER");

            }
            startService(intentResult);
            stopSelf();
    }


    @Nullable
    public IBinder onBind(Intent intent) {
        return binder;
    }


}
