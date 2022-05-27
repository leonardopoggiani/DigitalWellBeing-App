package it.unipi.dii.digitalwellbeing_app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;


public class ClassificationService extends Service {

    private String TAG = "ClassificationService";
    private ActivityClassifier classifier;

    Intent intentResult;
    Intent intentData;
    Intent intentSamplingRate;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Runnable toRun = () -> {
            classifier = new ActivityClassifier(this);
            intentData = intent;
            handleClassification();
        };
        Thread run = new Thread(toRun);
        run.start();
        return Service.START_STICKY;
    }

    private void handleClassification() {
        Float[] sampleArray = (Float[])intentData.getExtras().get("sampleArray");
        boolean activity = classifier.classifySamples(sampleArray);

        intentResult = new Intent("update_ui");
        intentSamplingRate = new Intent(getApplicationContext(), SensorHandler.class);
        intentSamplingRate.setAction("samplingRate");

        if(activity) {
            intentResult.putExtra("activity","PICKUP");
            intentSamplingRate.putExtra("activity", "PICKUP");
            startService(intentSamplingRate);
        }
        else {
            intentResult.putExtra("activity","OTHER");
        }

        getApplicationContext().sendBroadcast(intentResult);
    }


    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

}
