package it.unipi.dii.digitalwellbeing_app.ui;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

import it.unipi.dii.digitalwellbeing_app.Configuration;
import it.unipi.dii.digitalwellbeing_app.R;
import it.unipi.dii.digitalwellbeing_app.MainActivity;
import it.unipi.dii.digitalwellbeing_app.SensorHandler;

public class SwitchHandler implements View.OnClickListener{
    long[] vibrationPattern = new long []{ 100 , 200 , 300 , 400 , 500 , 400 , 300 , 200 , 400 };
    long[] noVibration = new long[]{};
    String TAG = "SwitchHandler";

    @Override
    public void onClick(View v) {
        SwitchMaterial vibration = v.findViewById(R.id.notification);

        if(vibration.isChecked()) {
            Log.d(TAG, "Vibration activated");

            MainActivity.builder.setVibrate(vibrationPattern);
            vibration.setText("Activated");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MainActivity.notificationManager.deleteNotificationChannel(Configuration.CHANNEL_ID);

                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel notificationChannel = new
                        NotificationChannel(Configuration.CHANNEL_ID, Configuration.ANDROID_CHANNEL_NAME, importance);
                MainActivity.builder.setChannelId(Configuration.CHANNEL_ID);

                MainActivity.notificationManager.createNotificationChannel(notificationChannel);

                MainActivity.builder.setVibrate(vibrationPattern);
                vibration.setText("Activated");
            }
        } else {
            MainActivity.builder.setVibrate(noVibration);
            vibration.setText("Not active");
        }
    }
}
