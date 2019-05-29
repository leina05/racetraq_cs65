package edu.dartmouth.cs.racetraq;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import edu.dartmouth.cs.racetraq.Adapters.BottomNavigationViewPagerAdapter;
import edu.dartmouth.cs.racetraq.Fragments.DashboardFragment;
import edu.dartmouth.cs.racetraq.Fragments.LiveGraphFragment;
import edu.dartmouth.cs.racetraq.Fragments.LiveMapFragment;
import edu.dartmouth.cs.racetraq.Models.DriveDatapoint;
import edu.dartmouth.cs.racetraq.Models.DriveEntryFB;
import edu.dartmouth.cs.racetraq.Fragments.SaveDriveDialogFragment;
import edu.dartmouth.cs.racetraq.Models.HomePageStats;
import edu.dartmouth.cs.racetraq.Services.BluetoothLeService;
import edu.dartmouth.cs.racetraq.Services.TrackingService;

public class NewDriveActivity extends AppCompatActivity implements ServiceConnection {

    private static final String TAG = "NewDriveActivity";
    // UI
    private ViewPager viewPager;
    private DashboardFragment dashFragment;
    private LiveMapFragment liveMapFragment;
    private LiveGraphFragment liveGraphFragment;

    // Service Constants
    public static final String UUID_UART_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";
    public static final int kTxMaxCharacters = 20;

    // Tracking Service Constants
    public static final String LOCATION_RECEIVE_ACTION = "edu.dartmouth.cs.racetraq.ACTIVITY_LOCATION_RECEIVE";
    public static final String LOCATION_KEY = "locationKey";


    // BLE Service Connection
    private BluetoothLeService mBluetoothLeService;
    private boolean isBoundBLE = false;
    private ServiceConnection bleConnection = this;
    private BroadcastReceiver bleDataReceiver;

    // Tracking Service Connection
    private boolean isBoundTracking = false;
    private ServiceConnection trackingConnection;
    private BroadcastReceiver mapDisplayReceiver;

    // Gatt
    private ArrayList<BluetoothGattCharacteristic> mGattCharacteristics;
    private BluetoothGattCharacteristic mRxCharacteristic;

    // Drive Data
    private DriveDatapoint driveDatapoint;
    private String mDriveTimeStamp;
    private double mDriveAvgSpeed = 0;
    private double mDriveTopSpeed = 0;
    private double mDriveDistance = 0;
    private ArrayList<LatLng> mLocationList = new ArrayList<>();
    private String mDriveName = "MyDrive";
    private long numDatapoints = 0;
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm", Locale.US);
    private SimpleDateFormat durationFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private Location lastLocation;
    private static final double KM_TO_MILE = 0.621371;
    private long endTime;

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mFirebaseAuth;

    // Home stats
    private int savedDrives = 0;
    private double milesDriven = 0;
    private double maxTopSpeed = 0;


    /** OVERRIDE METHODS **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_drive);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // UI

        BottomNavigationView navigationView = findViewById(R.id.bottom_navigation);
        viewPager = findViewById(R.id.viewpager);

        // create a fragment list in order.
        ArrayList<Fragment> fragments = new ArrayList<>();
        dashFragment = new DashboardFragment();
        liveMapFragment = new LiveMapFragment();
        liveGraphFragment = new LiveGraphFragment();

        fragments.add(dashFragment);
        fragments.add(liveMapFragment);
        fragments.add(liveGraphFragment);

        // bind ViewPager to Adapter
        BottomNavigationViewPagerAdapter adapter = new BottomNavigationViewPagerAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(adapter);

        // Create Bottom Navigation Listener
        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId())
                {
                    case(R.id.dash_tab):
                        item.setChecked(true);
                        viewPager.setCurrentItem(0);
                        break;
                    case(R.id.map_tab):
                        item.setChecked(false);
                        viewPager.setCurrentItem(1);
                        break;
                    case(R.id.plot_tab):
                        item.setChecked(false);
                        viewPager.setCurrentItem(2);

                }
                return false;
            }
        });

        mDriveTimeStamp = Long.toString(System.currentTimeMillis());


        // Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mFirebaseAuth.getCurrentUser();

        if (mUser != null)
        {
            String mUserID = "user_"+DriveActivity.EmailHash(mUser.getEmail());
            mDatabase.child(mUserID).child("home_stats").addChildEventListener(homeStatsListener);
        }


        // Bind to BLE service
        Intent bleIntent = new Intent(this, BluetoothLeService.class);

        if (BluetoothLeService.isRunning())
        {
            if (!isBoundBLE)
            {
                bindService(bleIntent, bleConnection, Context.BIND_AUTO_CREATE);
                isBoundBLE = true;
            }
        }

        // Bind to Tracking Service
        trackingConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        // start TrackingService
        Intent trackingIntent = new Intent(this, TrackingService.class);

        if (!TrackingService.isRunning())
        {
            startService(trackingIntent);
        }

        // bind to TrackingService
        bindService(trackingIntent, trackingConnection, Context.BIND_AUTO_CREATE);
        isBoundTracking = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_drive_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.end_drive_action)
        {
            // stop receiving ble Data
            if (bleDataReceiver != null)
            {
                unregisterReceiver(bleDataReceiver);
                bleDataReceiver = null;
            }
            // pause chronometer
            dashFragment.stopTime();
            endTime = System.currentTimeMillis();

            // end drive
            mShowDialog(R.string.drive_name);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bleDataReceiver != null)
        {
            unregisterReceiver(bleDataReceiver);
        }
        if (mapDisplayReceiver != null)
        {
            unregisterReceiver(mapDisplayReceiver);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // register receiver
        if (bleDataReceiver == null)
        {
            bleDataReceiver = new BleDataReceiver();
        }
        registerReceiver(bleDataReceiver, makeGattUpdateIntentFilter());

        // register broadcast receivers
        if (mapDisplayReceiver == null)
        {
            mapDisplayReceiver = new MapDisplayReceiver();
        }
        IntentFilter mapFilter = new IntentFilter();
        mapFilter.addAction(LOCATION_RECEIVE_ACTION);
        registerReceiver(mapDisplayReceiver, mapFilter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isBoundBLE)
        {
            unbindService(bleConnection);
        }
        mBluetoothLeService = null;

        if (isBoundTracking)
        {
            unbindService(trackingConnection);
        }

        if (TrackingService.isRunning())
        {
            stopService(new Intent(this, TrackingService.class));
        }
    }

    /** SERVICE METHODS **/

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

        mBluetoothLeService.discoverServices();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBluetoothLeService = null;

    }

    /** BROADCAST RECEIVERS **/

    /**
     * Broadcast Receiver for BLE intents
     */
    private class BleDataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {

            } else if (BluetoothLeService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                findGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                packageData(data);
            }
        }
    }

    /**
     * Receiver for Location updates
     */
    public class MapDisplayReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // get location and add to locationList
            Location location = intent.getParcelableExtra(LOCATION_KEY);
            mLocationList.add(new LatLng(location.getLatitude(), location.getLongitude()));

            if (lastLocation != null)
            {
                mDriveDistance += lastLocation.distanceTo(location)/1000;   // get distance in kms
            }

            liveMapFragment.updateDisplay(location);

            lastLocation = location;
        }
    }

    /**
     * Event listener for changes in Firebase exercise entries
     */
    ChildEventListener homeStatsListener = new ChildEventListener() {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            {
                if (dataSnapshot.getKey().equals("savedDrives"))
                {
                    savedDrives = Integer.parseInt((String) dataSnapshot.getValue());
                }
                else if (dataSnapshot.getKey().equals("milesDriven"))
                {
                    milesDriven = Double.parseDouble((String) dataSnapshot.getValue());
                }
                else if (dataSnapshot.getKey().equals("topSpeed"))
                {
                    maxTopSpeed = Double.parseDouble((String) dataSnapshot.getValue());
                }

            }

        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };

    /** PRIVATE METHODS **/

    private void findGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();

        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();

        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            // get UUID
            uuid = gattService.getUuid().toString();

            if (uuid.equals(UUID_UART_SERVICE))
            {
                // get GATT characteristics
                mGattCharacteristics = (ArrayList<BluetoothGattCharacteristic>) gattService.getCharacteristics();

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : mGattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equals(UUID_RX))
                    {
                        mRxCharacteristic = gattCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(mRxCharacteristic, true);
                    }
                }
            }

        }
    }

    private void packageData(String data) {
        String [] arr = data.split(",");
        if (arr.length == 5)
        {
            driveDatapoint = new DriveDatapoint(Long.toString(System.currentTimeMillis()));
            driveDatapoint.setTps(arr[0]);
            driveDatapoint.setSpeed(arr[1]);
            driveDatapoint.setEng_rpm(arr[2]);
            driveDatapoint.setEng_temp(arr[3]);
            driveDatapoint.setBatt_voltage(arr[4]);

            // update drive data
            double speed = Double.parseDouble(arr[1]);
            mDriveAvgSpeed += speed;
            if (speed > mDriveTopSpeed)
            {
                mDriveTopSpeed = speed;
            }
            numDatapoints++;

            addFirebaseDatapoint(driveDatapoint);

            dashFragment.displayData(driveDatapoint);
        }

    }

    private void addFirebaseDatapoint(DriveDatapoint datapoint) {

        Log.d(TAG, "addFirebaseDatapoint");

        mDatabase.child("user_"+EmailHash(Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getEmail()))
                .child("drive_entries").child(mDriveTimeStamp).child("datapoints").push().setValue(datapoint)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // finished inserting
                        } else {
                            // insertion failed
                        }
                    }
                });

    }

    private void addFirebaseSummary(DriveEntryFB summary) {

        mDatabase.child("user_"+EmailHash(Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getEmail()))
                .child("drive_entries").child(mDriveTimeStamp).child("summary").setValue(summary)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // finished inserting
                            Toast.makeText(NewDriveActivity.this, "Drive Saved.", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            // insertion failed
                        }
                    }
                });

    }

    private void addFirebaseHomeStats(HomePageStats stats)
    {
        mDatabase.child("user_"+EmailHash(Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getEmail()))
                .child("home_stats").setValue(stats)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // finished inserting
                        } else {
                            // insertion failed
                        }
                    }
                });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void saveDrive() {
        mDriveAvgSpeed /= numDatapoints;

        DriveEntryFB driveEntryFB = new DriveEntryFB();
        driveEntryFB.setDriveName(mDriveName);
        driveEntryFB.setDriveAvgSpeed(Double.toString(mDriveAvgSpeed));
        driveEntryFB.setDriveDistance(Double.toString(mDriveDistance*KM_TO_MILE));
        driveEntryFB.setDriveTimeStamp(dateTimeFormat.format(Long.parseLong(mDriveTimeStamp)));
        driveEntryFB.setDriveTopSpeed(Double.toString(mDriveTopSpeed));
        driveEntryFB.setDriveDuration(millisToString(endTime - Long.parseLong(mDriveTimeStamp)));
        Gson gson = new Gson();
        String gps_trace = gson.toJson(mLocationList);
        driveEntryFB.setLocationList(gps_trace);
        driveEntryFB.setNumPoints(Long.toString(numDatapoints));

        // post drive to firebase
        addFirebaseSummary(driveEntryFB);

        // update home stats
        savedDrives++;
        milesDriven += mDriveDistance;
        if (mDriveTopSpeed > maxTopSpeed)
        {
            maxTopSpeed = mDriveTopSpeed;
        }

        HomePageStats stats = new HomePageStats(savedDrives, milesDriven, maxTopSpeed);
        addFirebaseHomeStats(stats);
    }

    /**
     * Show dialog fragment
     */
    private void mShowDialog(int title_id) {
        DialogFragment newFragment = SaveDriveDialogFragment.createInstance(title_id);
        newFragment.setCancelable(false);
        newFragment.show(getSupportFragmentManager(), "dialog");
    }

    private String millisToString(long millis)
    {
        long mill = millis % 1000;
        long seconds = millis/1000;
        long s = seconds % 60;
        long m = (seconds / 60) % 60;
        long h = (seconds /60) / 60;

        String ts = String.format("%02d:%02d:%02d.%03d", h, m, s, mill);

        return ts;
    }

    /** PUBLIC METHODS **/

    public static String EmailHash(String email) {

        MessageDigest mDigest = null;
        try {
            mDigest = MessageDigest.getInstance("MD5");

            mDigest.update(email.getBytes());
            byte messageDigestArray[] = mDigest.digest();

            StringBuffer hex = new StringBuffer();
            for (int i = 0; i < messageDigestArray.length; i++) {
                hex.append(Integer.toHexString(0xFF & messageDigestArray[i]));
            }
            return hex.toString();

        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return " ";
    }

    /**
     * Save function for all dialogs
     */
    public void saveDialogEntry(int title_id, String data)
    {
        switch(title_id)
        {
            case R.string.drive_name:
                mDriveName = data;
                saveDrive();
            default:
                break;
        }
    }

    public void resumeDrive() {
        // register receiver
        if (bleDataReceiver == null)
        {
            bleDataReceiver = new BleDataReceiver();
        }
        registerReceiver(bleDataReceiver, makeGattUpdateIntentFilter());

        dashFragment.startTime();
    }

    public long getStartTime() {
        return Long.parseLong(mDriveTimeStamp);
    }

}
