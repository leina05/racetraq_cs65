package edu.dartmouth.cs.racetraq;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import edu.dartmouth.cs.racetraq.Services.BluetoothLeService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection {

    // UI
    private Button mNewDriveButton;
    private Button mBLEConnectButton;
    private TextView mConnectionStatusTextView;

    // BLE
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> connectedDevices;
    private boolean deviceConnected = false;

    // Service Connection
    private boolean isBound = false;
    private ServiceConnection mConnection = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                Intent intent = new Intent(MainActivity.this, TrackActivity.class);
                startActivity(intent);
            }
        });

        mBLEConnectButton = findViewById(R.id.ble_connection_button);
        mBLEConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceConnected)
                {
                    if (BluetoothLeService.isRunning())
                    {
                        stopService(new Intent(MainActivity.this, BluetoothLeService.class));
                    }
                }
                else
                {
                    Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
                    startActivity(intent);
                }

            }
        });

        /* Set up Bluetooth */
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        connectedDevices = (ArrayList<BluetoothDevice>) bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (connectedDevices != null && !connectedDevices.isEmpty())
        {
            deviceConnected = true;
            mBLEConnectButton.setText("Disconnect");
            String deviceName = connectedDevices.get(0).getName();
            mConnectionStatusTextView.setText(String.format("Connected to device: %s", deviceName == null ? connectedDevices.get(0).getAddress() : deviceName));
        }

        // start BluetoothLeService
        Intent bleIntent = new Intent(this, BluetoothLeService.class);

        if (BluetoothLeService.isRunning())
        {
            // bind to TrackingService
            bindService(bleIntent, mConnection, Context.BIND_AUTO_CREATE);
            isBound = true;
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

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
