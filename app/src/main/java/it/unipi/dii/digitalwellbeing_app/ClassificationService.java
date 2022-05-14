package it.unipi.dii.digitalwellbeing_app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

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
        intentResult = new Intent(this, MainActivity.class);
        intentResult.setAction("Classification_Result");
        status = false;
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

}
