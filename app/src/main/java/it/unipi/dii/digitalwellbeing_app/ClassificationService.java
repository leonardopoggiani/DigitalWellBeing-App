package it.unipi.dii.digitalwellbeing_app;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class ClassificationService {

    private final IBinder binder = new LocalBinder();
    private ServiceCallbacks serviceCallbacks;

    //Class used for the client Binder
    public class LocalBinder extends Binder {
        ClassificationService getService() {
            return ClassificationService.this;
        }
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

}
