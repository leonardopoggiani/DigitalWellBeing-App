package it.unipi.dii.digitalwellbeing_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.view.Change;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BeaconForegroundService extends Service {

    public static final String TAG = BeaconForegroundService.class.getSimpleName();

    public static final String ACTION_DEVICE_DISCOVERED = "DEVICE_DISCOVERED_ACTION";
    private static final String STOP_SERVICE_ACTION = "STOP_SERVICE_ACTION";

    private static final String NOTIFICATION_CHANEL_NAME = "Kontakt SDK Samples";
    private static final String NOTIFICATION_CHANEL_ID = "scanning_service_channel_id";

    private ProximityManager proximityManager;
    private boolean isRunning; // Flag indicating if service is already running.
    private NotificationChannel channel;
    private NotificationManager notificationManager;
    private Notification notificationForeground;
    private DatabaseReference db;
    List<Beacon> beacon_list;
    private String device;
    private boolean notfound;
    Beacon lastbeacon;
    ChangeLastBeacon timer;


    public static Intent createIntent(final Context context) {
        return new Intent(context, BeaconForegroundService.class);
    }

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate() {
        super.onCreate();
        db  = FirebaseDatabase.getInstance("https://digitalwellbeing-83177-default-rtdb.europe-west1.firebasedatabase.app/").getReference();
        beacon_list = new ArrayList<>();
        setupProximityManager();
        isRunning = false;
        device = UUID.randomUUID().toString();
        lastbeacon = new Beacon();
        beacon_list.clear();
        notfound = true;
    }

    private void setupProximityManager() {
        // Create proximity manager instance
        proximityManager = ProximityManagerFactory.create(this);

        // Configure proximity manager basic options
        proximityManager.configuration()
                //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanPeriod(ScanPeriod.RANGING)
                //Using BALANCED for best performance/battery ratio
                .scanMode(ScanMode.BALANCED);

        // Set up iBeacon listener
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    private boolean checkCondition(Beacon b){
        if(notfound) {
            return false;
        }
        if(lastbeacon == null) return false;
        if(!b.getId().equals(lastbeacon.getId())) {
            return false;
        }
        if(b.getProximity().equals("FAR")) {
            return false;
        }
        if(b.getUserDevice().equals(lastbeacon.getUserDevice())) {
            return false;
        }
        if(b.getTimestamp() < lastbeacon.getTimestamp() - 300000 || b.getTimestamp() > lastbeacon.getTimestamp() + 300000 ) {
            return false;
        }

        return true;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        beacon_list.clear();
        if (STOP_SERVICE_ACTION.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Check if service is already active
        if (isRunning) {
            Toast.makeText(this, "Service is already running", Toast.LENGTH_SHORT).show();
            return START_STICKY;
        }


        db.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Toast.makeText(getApplicationContext(), "Qualcuno ha scritto nel db", Toast.LENGTH_SHORT).show();
                beacon_list.clear();
                Beacon beacon = new Beacon();
                for (DataSnapshot postSnapshot : dataSnapshot.child("Beacon").getChildren()) {

                    beacon.setProximity(postSnapshot.child("proximity").getValue(String.class));
                    beacon.setTimestamp(postSnapshot.child("timestamp").getValue(Long.class));
                    beacon.setRssi(postSnapshot.child("rssi").getValue(Integer.class));
                    beacon.setUserDevice(postSnapshot.child("userDevice").getValue(String.class));
                    beacon.setAddress(postSnapshot.child("address").getValue(String.class));
                    beacon.setId(postSnapshot.child("id").getValue(String.class));
                    beacon.setDistance(postSnapshot.child("distance").getValue(Double.class));
                    if (checkCondition(beacon)) {
                        if (beacon_list.isEmpty()) {
                            beacon_list.add(beacon);
                        }
                        else {
                            boolean insert = true;
                            for (int i = 0; i < beacon_list.size(); i++) {
                                if (beacon_list.get(i).getUserDevice().equals(beacon.getUserDevice()))
                                    insert = false;
                            }
                            if (insert) beacon_list.add(beacon);
                        }
                    }
                }
                int userDetected = 0;
                if (beacon_list.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Empty", Toast.LENGTH_SHORT).show();
                }
                else {
                    userDetected = beacon_list.size();
                    Toast.makeText(getApplicationContext(), "User detected:" + beacon_list.size(), Toast.LENGTH_SHORT).show();
                    // Create notification channel

                    
                }
                Intent intentCount = new Intent("update_ui");
                intentCount.putExtra("device_count", userDetected);
                // startService(intentCount);
                sendBroadcast(intentCount);


        }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });

        startInForeground();
        startScanning();
        isRunning = true;
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (proximityManager != null) {
            proximityManager.disconnect();
            proximityManager = null;
        }
        Toast.makeText(this, "Scanning service stopped", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    private void startInForeground() {
        // Create notification intent
        final Intent notificationIntent = new Intent();
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                0
        );

        // Create stop intent with action
        final Intent intent = BeaconForegroundService.createIntent(this);
        intent.setAction(STOP_SERVICE_ACTION);
        final PendingIntent stopIntent = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // Build notification
        final NotificationCompat.Action action = new NotificationCompat.Action(0, "Stop", stopIntent);
        notificationForeground = new NotificationCompat.Builder(this, NOTIFICATION_CHANEL_ID)
                .setContentTitle("Scan service")
                .setContentText("Actively scanning Beacons")
                .addAction(action)
                .setSmallIcon(R.drawable.healthcare)
                .setContentIntent(pendingIntent)
                .build();

        // Start foreground service
        startForeground(1, notificationForeground);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        channel = new NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                NOTIFICATION_CHANEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(channel);
    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
                //devicesCount = 0;
                Toast.makeText(BeaconForegroundService.this, "Scanning service started", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                onDeviceDiscovered(ibeacon);
                Log.i(TAG, "onIBeaconDiscovered: " + ibeacon.toString());
            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                super.onIBeaconLost(ibeacon, region);
                Log.e(TAG, "onIBeaconLost: " + ibeacon.toString());
            }
        };
    }

    public void insert (DatabaseReference db, Beacon beacon, Context context){
        //Saving the beacon object
        String pushKey = db.push().getKey();
        db.child("Beacon").push().setValue(beacon);

    }

    private void onDeviceDiscovered(final RemoteBluetoothDevice device) {
        Toast.makeText(getApplicationContext(), "Beacon", Toast.LENGTH_SHORT).show();
        if(!device.getProximity().toString().equals("FAR")){
            if(timer != null){
                if(timer.isAlive())
                    timer.interrupt();
            }
            notfound = false;
            if(lastbeacon == null) lastbeacon= new Beacon();
            lastbeacon.setAddress(device.getAddress());
            lastbeacon.setDistance(device.getDistance());
            lastbeacon.setId(device.getUniqueId());
            lastbeacon.setProximity(device.getProximity().toString());
            lastbeacon.setRssi(device.getRssi());
            lastbeacon.setTimestamp(device.getTimestamp());
            lastbeacon.setUserDevice(this.device);
            insert(db, lastbeacon, getApplicationContext());
            timer = new ChangeLastBeacon(lastbeacon.getTimestamp());
            timer.start();
        }
        else {
            notfound = true;
            lastbeacon = null;
        }
        

        //Send a broadcast with discovered device
        Intent intent = new Intent();
        intent.setAction(ACTION_DEVICE_DISCOVERED);
        sendBroadcast(intent);
    }


    public class ChangeLastBeacon extends Thread {
        long start;

        ChangeLastBeacon(long time){
            start = time;
        }
        public void run(){
            try {

                while(System.currentTimeMillis() < start + 300000)
                    sleep(start + 300000 - System.currentTimeMillis());
                notfound = true;
                lastbeacon = null;
            } catch (InterruptedException ie) {}
        }
    }

}


