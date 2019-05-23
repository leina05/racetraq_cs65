package edu.dartmouth.cs.racetraq;

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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import edu.dartmouth.cs.racetraq.Models.DriveEntry;
import edu.dartmouth.cs.racetraq.Services.BluetoothLeService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection {

    // Firebase
    private FirebaseAuth mAuth;


    // UI
    private Button mNewDriveButton;
    private Button mBLEConnectButton;
    private TextView mConnectionStatusTextView;
    private AlertDialog mStatusDialog;

    // BLE
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> connectedDevices;
    private boolean deviceConnected = false;

    // Service Connection
    private BluetoothLeService mBluetoothLeService;
    private boolean isBound = false;
    private ServiceConnection mConnection = this;
    private BleConnectionReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* Check firebase authentication */
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        }

        /* Set up Bluetooth */
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        /* Set up UI */
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mConnectionStatusTextView = findViewById(R.id.ble_connection_status);

        mNewDriveButton = findViewById(R.id.new_drive_button);
        mNewDriveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DriveActivity.class);
                startActivity(intent);
            }
        });

        // Set Connect Button OnClickListener
        mBLEConnectButton = findViewById(R.id.ble_connection_button);
        mBLEConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceConnected)
                {
                    if (BluetoothLeService.isRunning())
                    {
                        mBluetoothLeService.disconnect();
                        mBLEConnectButton.setEnabled(false);
                    }
                }
                else
                {
                    Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
                    startActivity(intent);
                }

            }
        });

        // register broadcast receivers
        if (broadcastReceiver == null)
        {
            broadcastReceiver = new BleConnectionReceiver();

            IntentFilter mapFilter = new IntentFilter();
            mapFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
            mapFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
            registerReceiver(broadcastReceiver, mapFilter);
        }

        // start BluetoothLeService
        Intent bleIntent = new Intent(this, BluetoothLeService.class);

        if (!BluetoothLeService.isRunning())
        {
            startService(bleIntent);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        updateUI();

        // start BluetoothLeService
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.edit_profile_drawer) {
            // Handle the camera action
        } else if (id == R.id.saved_drives_drawer) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        } else if (id == R.id.settings_drawer) {

        } else if (id == R.id.logout_drawer) {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBluetoothLeService = null;
        isBound = false;

    }

    /**
     * PRIVATE FUNCTIONS
     */
    private void updateUI() {

        connectedDevices = (ArrayList<BluetoothDevice>) bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (connectedDevices != null && !connectedDevices.isEmpty())
        {
            deviceConnected = true;
        }
        else
        {
            deviceConnected = false;
        }

        if (deviceConnected)
        {
            mBLEConnectButton.setText("Disconnect");
            String deviceName = connectedDevices.get(0).getName();
            mConnectionStatusTextView.setText(String.format("Connected to: %s", deviceName == null ? connectedDevices.get(0).getAddress() : deviceName));
        }
        else
        {
            mBLEConnectButton.setText("Connect");
            mConnectionStatusTextView.setText("No device connected.");
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
                updateUI();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                deviceConnected = false;
                mBLEConnectButton.setEnabled(true);
                updateUI();
            }
        }
    }
}
