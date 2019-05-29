package edu.dartmouth.cs.racetraq;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.dartmouth.cs.racetraq.Adapters.HistoryViewAdapter;
import edu.dartmouth.cs.racetraq.CustomViews.RecyclerTouchListener;
import edu.dartmouth.cs.racetraq.Models.DriveEntryFB;
import edu.dartmouth.cs.racetraq.Models.DriveEntry;

import static edu.dartmouth.cs.racetraq.Utils.Constants.storageURL;

public class HistoryActivity extends AppCompatActivity {

    public static final String DRIVE_ENTRY_ID_KEY = "drive_entry_id";
    public static final String DRIVE_NAME_KEY = "drive_name_key";
    public static final String NUM_POINTS_KEY = "num_points_key";
    public static final String BYTE_ARRAY_KEY = "byte_array_key";
    private static final long LOADING_TIMEOUT = 10000;  // timeout loading after 10 seconds
    private static final long ONE_MEGABYTE = 1024 * 1024;

    // UI
    private RecyclerView.Adapter mAdapter;
    private List<DriveEntry> driveEntryList = new ArrayList<>();
    private AlertDialog loadingDialog;
    private Handler handler;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private FirebaseUser mUser;
    private String mUserID;
    private String userEmail;
    private String driveId;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private boolean paused = false;

    // Drive Data
    private int savedDrives;
    private double milesDriven;
    private double topSpeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();
        mUser = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        if (mUser != null) {
            userEmail = mUser.getEmail();
            mUserID = "user_" + NewDriveActivity.EmailHash(userEmail);
            mRef.child(mUserID).child("drive_entries").addChildEventListener(driveEntryListener);
        }

        // Get Intent Extras
        savedDrives = getIntent().getIntExtra(MainActivity.SAVED_DRIVES_KEY, 0);
        milesDriven = getIntent().getDoubleExtra(MainActivity.MILES_DRIVEN_KEY, 0);
        topSpeed = getIntent().getDoubleExtra(MainActivity.TOP_SPEED_KEY, 0);

        // UI

        /* Set Back Button */
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            getSupportActionBar().setDisplayShowHomeEnabled(true);

            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }
        setTitle("Saved Drives");

        /* Set up recycler view */
        RecyclerView recyclerView = findViewById(R.id.history_view);
        recyclerView.setHasFixedSize(true);

        /* use a linear layout manager */
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        /* Add HistoryViewAdapter */
        mAdapter = new HistoryViewAdapter(driveEntryList, getApplicationContext());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);


        /* Set OnClickListener for Exercise items */
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {

            @Override
            public void onClick(View view, int position) {
                Intent intent = new Intent(HistoryActivity.this, DisplayDriveActivity.class);
                DriveEntry entry = driveEntryList.get(position);
                String id = Long.toString(entry.getTimeMillis());
                intent.putExtra(DRIVE_ENTRY_ID_KEY, id);
                intent.putExtra(DRIVE_NAME_KEY, entry.getName());
                intent.putExtra(NUM_POINTS_KEY, entry.getNumPoints());

                // Get bitmap as byte array
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                entry.getMap_thumbnail().compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                intent.putExtra(BYTE_ARRAY_KEY, byteArray);

                startActivity(intent);

            }
        }));

        if (savedDrives > 0) {
            // Loading dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Loading saved drives...");

            loadingDialog = builder.create();
            loadingDialog.setCanceledOnTouchOutside(false);
            loadingDialog.show();

            handler = new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadingDialog.cancel();
                    savedDrives = driveEntryList.size();
                    mAdapter.notifyDataSetChanged();
                    mRef.child(mUserID).child("home_stats").child("savedDrives").setValue(Integer.toString(savedDrives));
                }
            }, LOADING_TIMEOUT);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
    }

    /**
     * Event listener for changes in Firebase exercise entries
     */
    ChildEventListener driveEntryListener = new ChildEventListener() {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            // only listen when not paused so don't add entries before they are complete
            if (!paused)
            {
                // if no summary, delete entry
                if (dataSnapshot.child("summary").getValue() == null) {
                    dataSnapshot.getRef().removeValue();
                } else {
                    DriveEntry entry = snapshotToDriveEntry(dataSnapshot);
                    driveEntryList.add(entry);

                    DownloadMapTask task = new DownloadMapTask();
                    task.execute(driveEntryList.size()-1);
                }
            }

        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            // delete entries from UI when they have been removed
            String timestamp = (String) dataSnapshot.child("summary").child("driveTimeStamp").getValue();
            HistoryActivity.DeleteDriveTask task = new HistoryActivity.DeleteDriveTask();
            task.execute(timestamp);

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };


    /**
     * Convert Firebase DataSnapshot to DriveEntry
     */
    private DriveEntry snapshotToDriveEntry(DataSnapshot ds) {
        DriveEntryFB entry = new DriveEntryFB();

        entry.setDriveAvgSpeed((String) ds.child("summary").child("driveAvgSpeed").getValue());
        entry.setDriveDistance((String) ds.child("summary").child("driveDistance").getValue());
        entry.setDriveDuration((String) ds.child("summary").child("driveDuration").getValue());
        entry.setDriveName((String) ds.child("summary").child("driveName").getValue());
        entry.setDriveTimeStamp((String) ds.child("summary").child("driveTimeStamp").getValue());
        entry.setDriveTopSpeed((String) ds.child("summary").child("driveTopSpeed").getValue());
        entry.setLocationList((String) ds.child("summary").child("locationList").getValue());
        entry.setNumPoints((String) ds.child("summary").child("numPoints").getValue());

        DriveEntry finalEntry = new DriveEntry(entry, this);
        finalEntry.setTimeMillis(Long.parseLong(ds.getKey()));

        return finalEntry;
    }

    /**
     * ASYNC TASKS
     **/

    /**
     * Background task to delete drive entry from entryList
     */
    class DeleteDriveTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... strings) {
            String timestamp = strings[0];
            double newTopSpeed = 0;
            int delete = -1;

            for (int i = 0; i < driveEntryList.size(); i++) {
                if (driveEntryList.get(i).getDateTime().equals(timestamp)) {
                    delete = i;
                } else if (driveEntryList.get(i).getTopSpeed() > newTopSpeed) {
                    newTopSpeed = driveEntryList.get(i).getTopSpeed();
                }
            }

            // delete entry;
            if (delete >= 0) {
                // update home stats
                savedDrives--;
                topSpeed = newTopSpeed;
                milesDriven -= driveEntryList.get(delete).getDistance();
                driveEntryList.remove(delete);

                mRef.child(mUserID).child("home_stats").child("savedDrives").setValue(Integer.toString(savedDrives));
                mRef.child(mUserID).child("home_stats").child("milesDriven").setValue(Double.toString(milesDriven));
                mRef.child(mUserID).child("home_stats").child("topSpeed").setValue(Double.toString(topSpeed));
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Background task to download map snapshot from Firebase Storage
     */
    class DownloadMapTask extends AsyncTask<Integer, Void, Void> {


        @Override
        protected Void doInBackground(Integer... integers) {
            final int index = integers[0];
            DriveEntry entry = driveEntryList.get(index);
            String timestamp = Long.toString(entry.getTimeMillis());

            StorageReference mapRef = storageReference.child(mUserID).child("mapSnapshots/"+timestamp+".jpeg");

            mapRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    // Data for "images/island.jpg" is returns, use this as needed
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    int newHeight = (int) (0.857*bitmap.getWidth());
                    int y0 = (bitmap.getHeight()-newHeight)/2;
                    bitmap = Bitmap.createBitmap(bitmap, 0, y0, bitmap.getWidth(), newHeight);
                    driveEntryList.get(index).setMap_thumbnail(bitmap);

                    // if last picture to load, close loading dialog
                    if (index == savedDrives-1)
                    {
                        if (loadingDialog != null) {
                            loadingDialog.cancel();
                            handler.removeCallbacksAndMessages(null);
                        }
                        mAdapter.notifyDataSetChanged();
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // if last picture to load, close loading dialog
                    if (index == savedDrives-1)
                    {
                        if (loadingDialog != null) {
                            loadingDialog.cancel();
                            handler.removeCallbacksAndMessages(null);
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                }
            });

            return null;
        }
    }
}
