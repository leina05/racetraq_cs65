package edu.dartmouth.cs.racetraq;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.dartmouth.cs.racetraq.Models.DriveDatapoint;
import edu.dartmouth.cs.racetraq.Services.BluetoothLeService;

public class DriveActivity extends AppCompatActivity implements ServiceConnection {

    private final String LIST_UUID = "UUID";

    // Service Constants
    public static final String UUID_UART_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";
    public static final int kTxMaxCharacters = 20;

    // Service Connection
    private BluetoothLeService mBluetoothLeService;
    private boolean isBound = false;
    private ServiceConnection mConnection = this;
    private BroadcastReceiver broadcastReceiver;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
            if (!isBound)
            {
                bindService(bleIntent, mConnection, Context.BIND_AUTO_CREATE);
                isBound = true;
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        // register receiver
        if (broadcastReceiver == null)
        {
            broadcastReceiver = new BleDataReceiver();
        }
        registerReceiver(broadcastReceiver, makeGattUpdateIntentFilter());

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (broadcastReceiver != null)
        {
            unregisterReceiver(broadcastReceiver);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isBound)
        {
            unbindService(mConnection);
        }
        mBluetoothLeService = null;
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

    private void packageData(String data)
    {
        String [] arr = data.split(",");
        if (arr.length == 5)
        {
            driveDatapoint = new DriveDatapoint(Long.toString(System.currentTimeMillis()));
            driveDatapoint.setTps(arr[0]);
            driveDatapoint.setSpeed(arr[1]);
            driveDatapoint.setEng_rpm(arr[2]);
            driveDatapoint.setEng_temp(arr[3]);
            driveDatapoint.setBatt_voltage(arr[4]);

            displayData(driveDatapoint);
        }


    }
    private void displayData(DriveDatapoint datapoint)
    {
        tpsTextView.setText(datapoint.getTps());
        engRpmTextView.setText(datapoint.getEng_rpm());
        speedTextView.setText(datapoint.getSpeed());
        engTempTextView.setText(datapoint.getEng_temp());
        battVoltTextView.setText(datapoint.getBatt_voltage());
    }
}
