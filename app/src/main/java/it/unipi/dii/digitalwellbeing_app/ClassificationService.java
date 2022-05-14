package it.unipi.dii.digitalwellbeing_app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.TreeMap;

public class ClassificationService extends IntentService {

    private final IBinder binder = new LocalBinder();
    private ServiceCallbacks serviceCallbacks;
    private String TAG = "ClassificationService";
    private ActivityClassifier classifier;

    Intent intentResult;
    boolean status;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public ClassificationService() {
        super("ClassificationService");
    }

    //Class used for the client Binder
    public class LocalBinder extends Binder {
        ClassificationService getService() {
            return ClassificationService.this;
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");

        classifier = new ActivityClassifier(this);
        intentResult = new Intent(this, SensorHandler.class);
        intentResult.setAction("Classification_Result");
        status = false;
        return super.onStartCommand(intent, flags, startId);
    }


    /*private void handleClassification() {

            Float[] sampleArray = intentResult.getFloatArrayExtra("sampleArray");
            TreeMap<Long,Float[]> toBeClassified = new TreeMap<>();
            toBeClassified = intentResult.getParcelableExtra("treeMap");
            boolean activity = classifier.classifySamples(sampleArray, toBeClassified);
            if(activity) {
                if(serviceCallbacks != null) {
                    serviceCallbacks.setActivityAndCounter("PICKUP!");
                }
            }
            else if(!serviceCallbacks.getActivity().equals("OTHER!")) {
                serviceCallbacks.setActivity("OTHER!");
            }
            stopSelf();


            */
            /*if(activity) {
                Log.d(TAG, "PICKUP");
                intentResult.putExtra("activity","PICKUP");

            }
            else if(!activity) {
                Log.d(TAG,"OTHERS");
                intentResult.putExtra("activity","OTHERS");

            }
            startService(intentResult);*/
    //}

    @Nullable
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if(intent.getAction() != null && intent.getAction().compareTo("Classify")==0) {
                //handleClassification();

        }

    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

}
