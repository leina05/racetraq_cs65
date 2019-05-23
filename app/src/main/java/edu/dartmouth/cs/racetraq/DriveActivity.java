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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import edu.dartmouth.cs.racetraq.Models.DriveDatapoint;
import edu.dartmouth.cs.racetraq.Models.DriveEntry;
import edu.dartmouth.cs.racetraq.Services.BluetoothLeService;
import edu.dartmouth.cs.racetraq.Services.TrackingService;

public class DriveActivity extends AppCompatActivity implements ServiceConnection {

    private final String LIST_UUID = "UUID";

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

    // UI
    private TextView tpsTextView;
    private TextView engRpmTextView;
    private TextView speedTextView;
    private Chronometer mChronometer;
    private TextView engTempTextView;
    private TextView battVoltTextView;

    // Drive Data
    private DriveDatapoint driveDatapoint;
    private String mDriveTimeStamp;
    private double mDriveAvgSpeed = 0;
    private double mDriveTopSpeed = 0;
    private double mDriveDistance = 0;
    private ArrayList<LatLng> mLocationList = new ArrayList<>();
    private String mDriveName = "MyDrive";
    private int numDatapoints = 0;


    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mFirebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDriveTimeStamp = Long.toString(System.currentTimeMillis());

        // Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();

        // UI
        FloatingActionButton finish_button = findViewById(R.id.finish_drive_button);
        finish_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // end drive
            }
        });

        tpsTextView = findViewById(R.id.drive_tps);
        engRpmTextView = findViewById(R.id.drive_eng_rpm);
        speedTextView = findViewById(R.id.drive_speed);
        mChronometer = findViewById(R.id.drive_timer);
        engTempTextView = findViewById(R.id.drive_eng_temp);
        battVoltTextView = findViewById(R.id.drive_batt_voltage);

        mChronometer.start();


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
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

        mBluetoothLeService.discoverServices();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBluetoothLeService = null;

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

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
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
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
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
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

            addFirebaseEntry(driveDatapoint);

            displayData(driveDatapoint);
        }

    }

    private void saveDrive() {
        mDriveAvgSpeed /= numDatapoints;

        DriveEntry driveEntry = new DriveEntry();
        driveEntry.setDriveAvgSpeed(Double.toString(mDriveAvgSpeed));


    }

    private void addFirebaseEntry(DriveDatapoint datapoint) {

        mDatabase.child("user_"+EmailHash(Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getEmail()))
                .child("drive_entries").child(mDriveTimeStamp).push().setValue(datapoint)
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

    private void displayData(DriveDatapoint datapoint) {
        tpsTextView.setText(datapoint.getTps());
        engRpmTextView.setText(datapoint.getEng_rpm());
        speedTextView.setText(datapoint.getSpeed());
        engTempTextView.setText(datapoint.getEng_temp());
        battVoltTextView.setText(datapoint.getBatt_voltage());
    }


    public String EmailHash(String email) {

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

}

