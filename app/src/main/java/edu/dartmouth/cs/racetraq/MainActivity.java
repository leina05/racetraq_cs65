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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import edu.dartmouth.cs.racetraq.Services.BluetoothLeService;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ServiceConnection {

    public static final String SAVED_DRIVES_KEY = "saved_drives_key";
    public static final String MILES_DRIVEN_KEY = "miles_driven_key";
    public static final String TOP_SPEED_KEY = "top_speed_key";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private FirebaseUser mUser;
    private String mUserID;
    private String userEmail;


    // UI
    private Button mNewDriveButton;
    private Button mBLEConnectButton;
    private TextView mConnectionStatusTextView;
    private AlertDialog alertDialog;
    private TextView numDrivesTextView;
    private TextView milesDrivenTextView;
    private TextView topSpeedTextView;

    // BLE
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice;
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

        numDrivesTextView = findViewById(R.id.num_saved_drives_text);
        milesDrivenTextView = findViewById(R.id.num_miles_driven);
        topSpeedTextView = findViewById(R.id.top_speed_num);


        /* Check firebase authentication */
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        }
        else
        {
            mDatabase = FirebaseDatabase.getInstance();
            mRef = mDatabase.getReference();
            mUser = mAuth.getCurrentUser();

            if (mUser != null)
            {
                userEmail = mUser.getEmail();
                mUserID = "user_"+NewDriveActivity.EmailHash(userEmail);
                mRef.child(mUserID).child("home_stats").addChildEventListener(homeStatsListener);
            }
        }



        /* Set up Bluetooth */
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // UI

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
                if (!deviceConnected)
                {
                    showStatusDialog(true, R.string.not_connected);
                }
                else
                {
                    Intent intent = new Intent(MainActivity.this, NewDriveActivity.class);
                    startActivity(intent);
                }

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
                        showStatusDialog(true, R.string.disconnecting);
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
            if (numDrivesTextView != null)
            {
                intent.putExtra(SAVED_DRIVES_KEY, Integer.parseInt(numDrivesTextView.getText().toString()));
            }
            if (milesDrivenTextView != null)
            {
                intent.putExtra(MILES_DRIVEN_KEY, Double.parseDouble(milesDrivenTextView.getText().toString()));
            }
            if (topSpeedTextView != null)
            {
                intent.putExtra(TOP_SPEED_KEY, Double.parseDouble(topSpeedTextView.getText().toString()));
            }
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
     * Event listener for changes in Firebase exercise entries
     */
    ChildEventListener homeStatsListener = new ChildEventListener() {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            if (dataSnapshot.getKey().equals("savedDrives"))
            {
                numDrivesTextView.setText((String) dataSnapshot.getValue());
            }
            else if (dataSnapshot.getKey().equals("milesDriven"))
            {
                double miles = Double.parseDouble((String) dataSnapshot.getValue());
                milesDrivenTextView.setText(String.format("%.2f", miles));

            }
            else if (dataSnapshot.getKey().equals("topSpeed"))
            {
                topSpeedTextView.setText((String) dataSnapshot.getValue());
            }

        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            if (dataSnapshot.getKey().equals("savedDrives"))
            {
                numDrivesTextView.setText((String) dataSnapshot.getValue());
            }
            else if (dataSnapshot.getKey().equals("milesDriven"))
            {
                double miles = Double.parseDouble((String) dataSnapshot.getValue());
                milesDrivenTextView.setText(String.format("%.2f", miles));

            }
            else if (dataSnapshot.getKey().equals("topSpeed"))
            {
                topSpeedTextView.setText((String) dataSnapshot.getValue());
            }
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

    /**
     * Broadcast Receiver for GattCallback connection events
     */
    private class BleConnectionReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                ArrayList<BluetoothDevice> connectedDevices = (ArrayList<BluetoothDevice>) bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
                if (connectedDevices != null && !connectedDevices.isEmpty())
                {
                    deviceConnected = true;
                    connectedDevice = connectedDevices.get(0);
                }
                updateUI();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                deviceConnected = false;
                mBLEConnectButton.setEnabled(true);
                showStatusDialog(false, R.string.disconnecting);
                updateUI();
            }
        }
    }

    /**
     * PRIVATE FUNCTIONS
     */
    private void updateUI() {

        if (deviceConnected)
        {
            mBLEConnectButton.setText("Disconnect");
            String deviceName = connectedDevice.getName();
            mConnectionStatusTextView.setText(String.format("Connected to: %s", deviceName == null ? connectedDevice.getAddress() : deviceName));
        }
        else
        {
            mBLEConnectButton.setText("Connect");
            mConnectionStatusTextView.setText("No device connected.");
        }
    }

    private void showStatusDialog(boolean show, int stringId) {
        if (show) {

            // Remove if a previous dialog was open (maybe because was clicked 2 times really quick)
            if (alertDialog != null) {
                alertDialog.cancel();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(stringId);

            if (stringId == R.string.not_connected)
            {
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.cancel();
                    }
                });
            }

            // Show dialog
            alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        } else {
            if (alertDialog != null) {
                alertDialog.cancel();
            }
        }
    }

}
