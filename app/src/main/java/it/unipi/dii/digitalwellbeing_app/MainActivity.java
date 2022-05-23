package it.unipi.dii.digitalwellbeing_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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

import it.unipi.dii.digitalwellbeing_app.ui.DarkModeHandler;
import it.unipi.dii.digitalwellbeing_app.ui.SwitchHandler;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener{

    private SensorHandler sensorHandlerService;
    private ClassificationService classificationService;
    private static final String TAG = "DigitalWellBeing";
    public static final int REQUEST_CODE_PERMISSIONS = 100;
    boolean bound = false;
    private Context ctx;
    public static int statusBarNotificationID;
    static public NotificationCompat.Builder builder;
    static public NotificationManager notificationManager;
    public static int PICKUP_LIMIT = Configuration.PICKUP_LIMIT_DEFAULT;
    boolean already_notified = false;
    private final SwitchHandler switchHandler = new SwitchHandler();
    private final DarkModeHandler darkModeHandler = new DarkModeHandler();
    private int group = 0;

    private static MainActivity instance;
    private BroadcastReceiver broadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SwitchMaterial switchtheme = findViewById(R.id.switchtheme);
        switchtheme.setOnClickListener(darkModeHandler);

        instance = this;

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

        registerBroadcastReceiver();

        // Create an Intent for the activity you want to start
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int)System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this, Configuration.CHANNEL_ID)
                .setContentTitle("DigitalWellBeing Alert")
                .setContentIntent(pendingIntent)
                .setContentText("You have picked your phone " + count + " times.")
                .setSmallIcon(R.drawable.healthcare)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false);

        createNotificationChannel();
        notificationManager.notify(statusBarNotificationID, builder.build());
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    public static MainActivity getInstance() {
        return instance;
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
                TextView tv3 = findViewById(R.id.activity_number);
                tv3.setText(String.valueOf(count));
                Log.d(TAG, String.valueOf(count));

                TextView tv4 = findViewById(R.id.perc);
                int percentuale = Integer.parseInt((String) tv4.getText());
                int how_many_in_groups = (percentuale * (count - 1)) / 100;
                Log.d(TAG, "Group: " + how_many_in_groups);

                if(group == 0 || group == 1) {
                    tv4.setText(String.valueOf( ( (how_many_in_groups) * 100) / count) );
                } else {
                    // sono in un gruppo
                    tv4.setText(String.valueOf( ( (how_many_in_groups + 1) * 100) / count) );
                }
            }

        });
    }

    private void registerBroadcastReceiver(){
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceive della broadcast");
                if(intent.getAction() != null && intent.getAction().equals("update_ui")) {

                    if (intent.hasExtra("device_count")) {
                        int device_count = intent.getIntExtra("device_count", 0);
                        Log.d(TAG, "device count");
                        group = device_count;
                    } else {
                        String activity = intent.getStringExtra("activity");
                        Log.d(TAG, activity);
                        if (activity.equals("OTHER")) {
                            setActivity(activity);
                        } else {
                            setActivityAndCounter(activity);
                        }
                    }
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

    public void startScanning(){
        checkPermissions();
        Intent intent = BeaconForegroundService.createIntent(this);
        startService(intent);
    }

    public void stopScanning(){
        Intent intent = BeaconForegroundService.createIntent(this);
        stopService(intent);
    }

    private void checkPermissions() {
        String[] requiredPermissions = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                ? new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}
                : new String[]{ android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION };
        if(isAnyOfPermissionsNotGranted(requiredPermissions)) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean isAnyOfPermissionsNotGranted(String[] requiredPermissions){
        for(String permission: requiredPermissions){
            int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, permission);
            if(PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (REQUEST_CODE_PERMISSIONS == requestCode) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location permissions are mandatory to use BLE features on Android 6.0 or higher", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {
        Button start_button = findViewById(R.id.start);

        if(start_button.getText().toString().equals("START")) {
            startScanning();
            Intent startIntent = new Intent(this, SensorHandler.class);
            startIntent.setAction("Command");
            startIntent.putExtra("command_key", "START");
            startService(startIntent);
            start_button.setText("STOP");
        } else if(start_button.getText() == "STOP") {
            stopScanning();
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
        limit.setText("" + (progress*10 + 10) );

        MainActivity.PICKUP_LIMIT = progress*10 + 10;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    private final BroadcastReceiver scanningBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Device discovered!
            //int devicesCount = intent.getIntExtra(BackgroundScanService.EXTRA_DEVICES_COUNT, 0);
            //RemoteBluetoothDevice device = intent.getParcelableExtra(BackgroundScanService.EXTRA_DEVICE);
            //statusText.setText(String.format("Total discovered devices: %d\n\nLast scanned device:\n%s", devicesCount, device.toString()));
            Toast.makeText(context, "Beacon detected", Toast.LENGTH_LONG).show();
        }
    };
}




