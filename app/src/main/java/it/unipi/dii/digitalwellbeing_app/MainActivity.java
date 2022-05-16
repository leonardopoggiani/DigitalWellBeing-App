package it.unipi.dii.digitalwellbeing_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import it.unipi.dii.digitalwellbeing_app.ui.SwitchHandler;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks, View.OnClickListener, SeekBar.OnSeekBarChangeListener{

    private SensorHandler sensorHandlerService;
    private ClassificationService classificationService;
    private static final String TAG = "DigitalWellBeing";
    boolean bound = false;
    private Context ctx;
    String CHANNEL_ID = "notification";
    int statusBarNotificationID;
    public static final String ANDROID_CHANNEL_NAME = "ANDROID CHANNEL";
    static public NotificationCompat.Builder builder;
    NotificationManager notificationManager;
    int PICKUP_LIMIT = Configuration.PICKUP_LIMIT_DEFAULT;
    boolean already_notified = false;
    private final SwitchHandler switchHandler = new SwitchHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intentSensorHandler = new Intent(this, SensorHandler.class);
        bindService(intentSensorHandler, serviceConnection, Context.BIND_AUTO_CREATE);

        Button start = (Button) findViewById(R.id.start);
        start.setOnClickListener(this);

        SeekBar pickup_limit = findViewById(R.id.limit_seekbar);
        pickup_limit.setOnSeekBarChangeListener(this);
        pickup_limit.setProgress(5);

        TextView limit = findViewById(R.id.limit);
        limit.setText("" + PICKUP_LIMIT);

        SwitchMaterial notifications = findViewById(R.id.notification);
        notifications.setOnClickListener(switchHandler);

        TextView tv2 = findViewById(R.id.counter);
        CharSequence counter = tv2.getText();
        int count = Integer.parseInt(counter.toString());

        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("DigitalWellBeing Alert")
                .setContentText("You have picked your phone " + count + " times.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        createNotificationChannel();

        builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setVibrate(new long[]{0L});

        notificationManager.notify(statusBarNotificationID, builder.build());
    }

    public NotificationCompat.Builder getBuilder() {
        return builder;
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager. IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new
                    NotificationChannel( CHANNEL_ID , "NOTIFICATION_CHANNEL_NAME" , importance);
            notificationChannel.enableLights( true );
            notificationChannel.setLightColor( Color. RED );
            notificationChannel.enableVibration( true );
            notificationChannel.setVibrationPattern( new long []{});
            builder.setChannelId( CHANNEL_ID ) ;

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel) ;

        } else {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

    /**
     * Callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // cast the IBinder and get SensorHandler Service instance
            SensorHandler.LocalBinder binder = (SensorHandler.LocalBinder) service;
            sensorHandlerService = binder.getService();
            bound = true;
            sensorHandlerService.setCallbacks(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }

    };


    @Override
    public void setActivityAndCounter(String activity) {
        new Handler(Looper.getMainLooper()).post(() -> {
            TextView tv = findViewById(R.id.activity);
            TextView tv2 = findViewById(R.id.counter);

            tv.setText(activity);
            Log.d(TAG, activity);

            if(!activity.equals("OTHER")) {
                ImageView imageView = findViewById(R.id.activity_view);
                imageView.setImageResource(R.drawable.pickup);

                CharSequence counter = tv2.getText();
                int count = Integer.parseInt(counter.toString());
                count += 1;

                builder.setContentText("You have picked your phone " + count + " times.");
                // Because the ID remains unchanged, the existing notification is
                // updated.
                notificationManager.notify(
                        statusBarNotificationID,
                        builder.build());

                if(count > PICKUP_LIMIT && !already_notified) {
                    Toast.makeText(getApplicationContext(),"You are watching too much your phone!",Toast.LENGTH_LONG).show();
                    notificationManager.cancel(statusBarNotificationID);
                    builder.setColor(Color.RED);
                    builder.setContentText("You have picked your phone " + count + " times.");

                    notificationManager.notify(
                            statusBarNotificationID,
                            builder.build());

                    already_notified = true;
                }

                tv2.setText(String.valueOf(count));
                Log.d(TAG, String.valueOf(count));
            }

        });
    }

    @Override
    public String getActivity() {
        TextView tv = findViewById(R.id.activity);
        return tv.getText().toString();
    }

    @Override
    public void setActivity(String s) {
        TextView tv = findViewById(R.id.activity);

        tv.setText(s);
        Log.d(TAG, s);

        ImageView imageView = findViewById(R.id.activity_view);
        imageView.setImageResource(R.drawable.other);
    }

    @Override
    public void onClick(View v) {
        Button start_button = findViewById(R.id.start);

        if(start_button.getText().toString().equals("START")) {
            Log.d(TAG, "Start Smartwatch sensing");
            Intent startIntent = new Intent(this, SensorHandler.class);
            startIntent.setAction("Command");
            startIntent.putExtra("command_key", "START");
            startService(startIntent);
            start_button.setText("STOP");

        } else if(start_button.getText() == "STOP") {
            Log.d(TAG, "Stop sensing");
            Intent stopIntent = new Intent(this, SensorHandler.class);
            stopIntent.setAction("Command");
            stopIntent.putExtra("command_key", "STOP");
            startService(stopIntent);
            start_button.setText("START");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView limit = findViewById(R.id.limit);
        limit.setText("" + progress*10);

        if(progress != 0) {
            PICKUP_LIMIT = progress * 10;
        } else {
            PICKUP_LIMIT = 50;
            limit.setText("50");
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO document why this method is empty
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO document why this method is empty
    }


}




