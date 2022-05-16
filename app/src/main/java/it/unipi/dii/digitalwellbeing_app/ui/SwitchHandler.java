package it.unipi.dii.digitalwellbeing_app.ui;

import android.app.NotificationManager;
import android.util.Log;
import android.view.View;

import androidx.core.app.NotificationCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import it.unipi.dii.digitalwellbeing_app.R;
import it.unipi.dii.digitalwellbeing_app.MainActivity;

public class SwitchHandler implements View.OnClickListener{
    long[] vibrationPattern = new long []{ 100 , 200 , 300 , 400 , 500 , 400 , 300 , 200 , 400 };
    long[] noVibration = new long[]{};
    String TAG = "SwitchHandler";

    @Override
    public void onClick(View v) {
        SwitchMaterial vibration = v.findViewById(R.id.notification);

        if(vibration.isChecked()) {
            Log.d(TAG, "Vibration activated");
            /*
            notificationChannel.enableVibration( true );
            notificationChannel.setVibrationPattern( new long []{ 100 , 200 , 300 , 400 , 500 , 400 , 300 , 200 , 400 });
             */

            MainActivity.builder.setVibrate(vibrationPattern);
            vibration.setText("Activated");
        } else {
            MainActivity.builder.setVibrate(noVibration);
            vibration.setText("Not active");
        }
    }
}
