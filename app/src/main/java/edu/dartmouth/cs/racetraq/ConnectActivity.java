package edu.dartmouth.cs.racetraq;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import edu.dartmouth.cs.racetraq.Models.BluetoothDeviceData;
import edu.dartmouth.cs.racetraq.Services.BluetoothLeService;

public class ConnectActivity extends AppCompatActivity implements ServiceConnection {

    private final static String TAG = ConnectActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String DEVICE_ADDRESS_KEY = "device_address";


    // BLE
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private boolean mScanning;
    private Handler handler;
    private ArrayList<BluetoothDeviceData> mScannedDevices = new ArrayList<>();
    private BluetoothDeviceData mSelectedDeviceData;
    private boolean deviceConnected = false;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;


    // Stops scanning after 10 seconds.
    private long mLastUpdateMillis;
    private final static long kMinDelayToUpdateUI = 200;    // in milliseconds
    private static final long SCAN_PERIOD = 10000;

    // UI
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView.Adapter mScannedDevicesAdapter;
    private AlertDialog mConnectingDialog;
    private RecyclerView devicesRecyclerView;
    private TextView noDevicesTextView;


    // Service Connection
    private boolean isBound = false;
    private ServiceConnection mConnection = this;
    BleConnectionReceiver broadcastReceiver;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* Set Back Button */
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            getSupportActionBar().setDisplayShowHomeEnabled(true);

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        // Initializes Bluetooth adapter.
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        handler = new Handler();

        noDevicesTextView = findViewById(R.id.connect_no_devices);

        /* Set up recycler view */
        devicesRecyclerView = findViewById(R.id.connect_view);
        devicesRecyclerView.setHasFixedSize(true);

        /* use a linear layout manager */
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        devicesRecyclerView.setLayoutManager(layoutManager);

        /* Add HistoryViewAdapter */
        mScannedDevicesAdapter = new ConnectViewAdapter(mScannedDevices);
        devicesRecyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        devicesRecyclerView.setAdapter(mScannedDevicesAdapter);

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mScannedDevices.clear();
                startScan(null);

                mSwipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 500);
            }
        });
        requestLocationPermissionIfNeeded();

    }

    @Override
    public void onResume() {
        super.onResume();

        // Autostart scan
        autostartScan();

        // Update UI
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(isBound)
        {
            unbindService(mConnection);
            isBound = false;
        }

        if (broadcastReceiver != null)
        {
            unregisterReceiver(broadcastReceiver);
        }
    }

    // region Permissions
    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can scan for Bluetooth peripherals");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission granted");
                    // Autostart scan
                    autostartScan();
                    // Update UI
                    updateUI();
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Bluetooth Scanning not available");
                    builder.setMessage("Since location access has not been granted, the app will not be able to scan for Bluetooth peripherals");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth was enabled, resume scanning
                    autostartScan();
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // TODO: enable bluetooth dialog
//                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                    AlertDialog dialog = builder.setMessage(R.string.dialog_error_no_bluetooth)
//                            .setPositiveButton(android.R.string.ok, null)
//                            .show();
//                    DialogUtils.keepDialogOnOrientationChanges(dialog);
                }
                break;
        }
    }

    private void autostartScan() {
        if (bluetoothAdapter.isEnabled()) {
            // Force restart scanning
            if (mScannedDevices != null) {      // Fixed a weird bug when resuming the app (this was null on very rare occasions even if it should not be)
                mScannedDevices.clear();
            }
            startScan(null);
        }
    }

    // region Scan
    private void startScan(final UUID[] servicesToScan) {
        Log.d(TAG, "startScan");

        // Stop current scanning (if needed)
        stopScanning();

        // Configure scanning
        if (!bluetoothAdapter.isEnabled()) {
            // Ensures Bluetooth is available on the device and it is enabled. If not,
            // displays a dialog requesting user permission to enable Bluetooth.
            Log.w(TAG, "startScan: BluetoothAdapter not initialized or unspecified address.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);

        }

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    BluetoothDeviceData previouslyScannedDeviceData = null;
                    if (mScannedDevices == null) {
                        mScannedDevices = new ArrayList<>();       // Safeguard
                    }

                    // only add devices with RSSI >= -80
                    if (rssi < -80) return;

                    // Check that the device was not previously found
                    for (BluetoothDeviceData deviceData : mScannedDevices) {
                        if (deviceData.getDevice().getAddress().equals(device.getAddress())) {
                            previouslyScannedDeviceData = deviceData;
                            break;
                        }
                    }

                    BluetoothDeviceData deviceData;
                    if (previouslyScannedDeviceData == null) {
                        // Add it to the mScannedDevice list
                        deviceData = new BluetoothDeviceData();
                        mScannedDevices.add(deviceData);
                    } else {
                        deviceData = previouslyScannedDeviceData;
                    }

                    deviceData.setDevice(device);
                    deviceData.setRssi(rssi);
                    deviceData.setScanRecord(scanRecord);
                    decodeScanRecords(deviceData);

                    // Update device data
                    long currentMillis = SystemClock.uptimeMillis();
                    if (previouslyScannedDeviceData == null || currentMillis - mLastUpdateMillis > kMinDelayToUpdateUI) {          // Avoid updating when not a new device has been found and the time from the last update is really short to avoid updating UI so fast that it will become unresponsive
                        mLastUpdateMillis = currentMillis;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateUI();
                            }
                        });
                    }


                }
            };

    private void updateUI() {

        // Show list and hide "no devices" label
        final boolean isListEmpty = mScannedDevices == null || mScannedDevices.size() == 0;
        noDevicesTextView.setVisibility(isListEmpty ? View.VISIBLE : View.GONE);
        devicesRecyclerView.setVisibility(isListEmpty ? View.GONE : View.VISIBLE);

        // devices list
        mScannedDevicesAdapter.notifyDataSetChanged();
    }

    private void stopScanning() {
        // Stop scanning
        if (mScanning) {
            handler.removeCallbacksAndMessages(null);      // cancel pending calls to stop
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
            Log.d(TAG, "stop scanning");
        }

    }


    private void onClickDeviceConnectHelper(int scannedDeviceIndex) {
        stopScanning();

        if (scannedDeviceIndex < mScannedDevices.size()) {
            mSelectedDeviceData = mScannedDevices.get(scannedDeviceIndex);
            BluetoothDevice device = mSelectedDeviceData.getDevice();

            connectionState = STATE_CONNECTING;

            showStatusDialog(true, R.string.connecting);

            // start BluetoothLeService
            Intent bleIntent = new Intent(this, BluetoothLeService.class);
            bleIntent.putExtra(DEVICE_ADDRESS_KEY, device.getAddress());

            if (!BluetoothLeService.isRunning())
            {
                startService(bleIntent);
            }

            // bind to TrackingService
            if (!isBound)
            {
                bindService(bleIntent, mConnection, Context.BIND_AUTO_CREATE);
                isBound = true;
            }

            // register broadcast receivers
            if (broadcastReceiver == null)
            {
                broadcastReceiver = new BleConnectionReceiver();

                IntentFilter mapFilter = new IntentFilter();
                mapFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
                mapFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
                registerReceiver(broadcastReceiver, mapFilter);
            }


        } else {
            Log.w(TAG, "onClickDeviceConnect index does not exist: " + scannedDeviceIndex);
        }
    }

    /**
     * Broadcast Receiver for GattCallback connection events
     */
    private class BleConnectionReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                deviceConnected = true;
                connectionState = STATE_CONNECTED;
                showStatusDialog(true, R.string.device_connected);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                deviceConnected = false;
                connectionState = STATE_DISCONNECTED;
            }
        }
    }

    private void showStatusDialog(boolean show, int stringId) {
        if (show) {

            // Remove if a previous dialog was open (maybe because was clicked 2 times really quick)
            if (mConnectingDialog != null) {
                mConnectingDialog.cancel();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(stringId);

            if (stringId == R.string.device_connected)
            {
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }

            // Show dialog
            mConnectingDialog = builder.create();
            mConnectingDialog.setCanceledOnTouchOutside(false);
            mConnectingDialog.show();
        } else {
            if (mConnectingDialog != null) {
                mConnectingDialog.cancel();
            }
        }
    }

    /**
     * SERVICE METHODS
     **/
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    /**
     * VIEW ADAPTER
     **/
    public class ConnectViewAdapter extends RecyclerView.Adapter<ConnectViewAdapter.DeviceViewHolder>{

        private ArrayList<BluetoothDeviceData> deviceList;

        // Constructor
        public ConnectViewAdapter(ArrayList<BluetoothDeviceData> deviceList)
        {
            this.deviceList = deviceList;
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.ble_device_layout, viewGroup, false);
            return new DeviceViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ConnectViewAdapter.DeviceViewHolder deviceViewHolder, final int position) {
            BluetoothDeviceData deviceData = deviceList.get(position);

            deviceViewHolder.name.setText(deviceData.getNiceName());
            deviceViewHolder.rssi.setText("RSSI: " + deviceData.getRssi());

            deviceViewHolder.connect_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // call device connect function
                    onClickDeviceConnectHelper(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return deviceList.size();
        }

        public class DeviceViewHolder extends RecyclerView.ViewHolder {
            public TextView name, rssi;
            public Button connect_button;

            public DeviceViewHolder(View itemView) {
                super(itemView);

                name = itemView.findViewById(R.id.connect_device_name);
                connect_button = itemView.findViewById(R.id.connect_button);
                rssi = itemView.findViewById(R.id.connect_device_rssi);
            }
        }
    }


    /**
     * BLE SCAN METHODS
     */
    private void decodeScanRecords(BluetoothDeviceData deviceData) {
        // based on http://stackoverflow.com/questions/24003777/read-advertisement-packet-in-android
        final byte[] scanRecord = deviceData.getScanRecord();

        ArrayList<UUID> uuids = new ArrayList<>();
        byte[] advertisedData = Arrays.copyOf(scanRecord, scanRecord.length);
        int offset = 0;
        deviceData.type = BluetoothDeviceData.kType_Unknown;

        // Check if is an iBeacon ( 0x02, 0x0x1, a flag byte, 0x1A, 0xFF, manufacturer (2bytes), 0x02, 0x15)
        final boolean isBeacon = advertisedData[0] == 0x02 && advertisedData[1] == 0x01 && advertisedData[3] == 0x1A && advertisedData[4] == (byte) 0xFF && advertisedData[7] == 0x02 && advertisedData[8] == 0x15;

        // Check if is an URIBeacon
        final byte[] kUriBeaconPrefix = {0x03, 0x03, (byte) 0xD8, (byte) 0xFE};
        final boolean isUriBeacon = Arrays.equals(Arrays.copyOf(scanRecord, kUriBeaconPrefix.length), kUriBeaconPrefix) && advertisedData[5] == 0x16 && advertisedData[6] == kUriBeaconPrefix[2] && advertisedData[7] == kUriBeaconPrefix[3];

        if (isBeacon) {
            deviceData.type = BluetoothDeviceData.kType_Beacon;

            // Read uuid
            offset = 9;
            UUID uuid = getUuidFromByteArrayBigEndian(Arrays.copyOfRange(scanRecord, offset, offset + 16));
            uuids.add(uuid);
            offset += 16;

            // Skip major minor
            offset += 2 * 2;   // major, minor

            // Read txpower
            final int txPower = advertisedData[offset++];
            deviceData.setTxPower(txPower);
        } else if (isUriBeacon) {
            deviceData.type = BluetoothDeviceData.kType_UriBeacon;

            // Read txpower
            final int txPower = advertisedData[9];
            deviceData.setTxPower(txPower);
        } else {
            // Read standard advertising packet
            while (offset < advertisedData.length - 2) {
                // Length
                int len = advertisedData[offset++];
                if (len == 0) break;

                // Type
                int type = advertisedData[offset++];
                if (type == 0) break;

                // Data
//            Log.d(TAG, "record -> lenght: " + length + " type:" + type + " data" + data);

                switch (type) {
                    case 0x02:          // Partial list of 16-bit UUIDs
                    case 0x03: {        // Complete list of 16-bit UUIDs
                        while (len > 1) {
                            int uuid16 = advertisedData[offset++] & 0xFF;
                            uuid16 |= (advertisedData[offset++] << 8);
                            len -= 2;
                            uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                        }
                        break;
                    }

                    case 0x06:          // Partial list of 128-bit UUIDs
                    case 0x07: {        // Complete list of 128-bit UUIDs
                        while (len >= 16) {
                            try {
                                // Wrap the advertised bits and order them.
                                UUID uuid = getUuidFromByteArraLittleEndian(Arrays.copyOfRange(advertisedData, offset, offset + 16));
                                uuids.add(uuid);

                            } catch (IndexOutOfBoundsException e) {
                                Log.e(TAG, "BlueToothDeviceFilter.parseUUID: " + e.toString());
                            } finally {
                                // Move the offset to read the next uuid.
                                offset += 16;
                                len -= 16;
                            }
                        }
                        break;
                    }

                    case 0x09: {
                        byte[] nameBytes = new byte[len - 1];
                        for (int i = 0; i < len - 1; i++) {
                            nameBytes[i] = advertisedData[offset++];
                        }

                        String name = null;
                        try {
                            name = new String(nameBytes, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        deviceData.setAdvertisedName(name);
                        break;
                    }

                    case 0x0A: {        // TX Power
                        final int txPower = advertisedData[offset++];
                        deviceData.setTxPower(txPower);
                        break;
                    }

                    default: {
                        offset += (len - 1);
                        break;
                    }
                }
            }

            // Check if Uart is contained in the uuids
            boolean isUart = false;
            for (UUID uuid : uuids) {
                if (uuid.toString().equalsIgnoreCase(UUID_SERVICE)) {
                    isUart = true;
                    break;
                }
            }
            if (isUart) {
                deviceData.type = BluetoothDeviceData.kType_Uart;
            }
        }

        deviceData.setUuids(uuids);
    }

    public static UUID getUuidFromByteArraLittleEndian(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        long high = bb.getLong();
        long low = bb.getLong();
        UUID uuid = new UUID(low, high);
        return uuid;
    }

    public static UUID getUuidFromByteArrayBigEndian(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        UUID uuid = new UUID(high, low);
        return uuid;
    }
}
