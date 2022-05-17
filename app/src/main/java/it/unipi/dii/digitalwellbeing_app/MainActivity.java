package it.unipi.dii.digitalwellbeing_app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener{

    private SensorHandler sensorHandlerService;
    private ClassificationService classificationService;
    private static final String TAG = "DigitalWellBeing";
    boolean bound = false;
    private Context ctx;
    public static int statusBarNotificationID;
    static public NotificationCompat.Builder builder;
    static public NotificationManager notificationManager;
    public static int PICKUP_LIMIT = Configuration.PICKUP_LIMIT_DEFAULT;
    boolean already_notified = false;
    private final SwitchHandler switchHandler = new SwitchHandler();

    private BroadcastReceiver broadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerBroadcastReceiver();

        Intent intentSensorHandler = new Intent(this, SensorHandler.class);
        //bindService(intentSensorHandler, serviceConnection, Context.BIND_AUTO_CREATE);

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

        builder = new NotificationCompat.Builder(this, Configuration.CHANNEL_ID)
                .setContentTitle("DigitalWellBeing Alert")
                .setContentText("You have picked your phone " + count + " times.")
                .setSmallIcon(R.drawable.healthcare)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        createNotificationChannel();
        notificationManager.notify(statusBarNotificationID, builder.build());

    }

    public NotificationCompat.Builder getBuilder() {
        return builder;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel notificationChannel = new
                    NotificationChannel(Configuration.CHANNEL_ID , Configuration.ANDROID_CHANNEL_NAME , importance);
            builder.setChannelId(Configuration.CHANNEL_ID) ;
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel) ;

        } else {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }


    /*
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

    };*/


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

    private void registerBroadcastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive della broadcast");
                if(intent.getAction() != null && intent.getAction().equals("update_ui")){
                    String activity = intent.getStringExtra("activity");
                   setActivityAndCounter(activity);
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("update_ui"));
    }


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
            MainActivity.PICKUP_LIMIT = progress * 10;
        } else {
            MainActivity.PICKUP_LIMIT = 50;
            limit.setText("50");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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




