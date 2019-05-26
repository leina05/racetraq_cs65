package edu.dartmouth.cs.racetraq;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import edu.dartmouth.cs.racetraq.Adapters.HistoryViewAdapter;
import edu.dartmouth.cs.racetraq.CustomViews.RecyclerTouchListener;
import edu.dartmouth.cs.racetraq.Models.DriveEntry;
import edu.dartmouth.cs.racetraq.Models.MockDriveEntry;

public class HistoryActivity extends AppCompatActivity {

    // UI
    private RecyclerView.Adapter mAdapter;
    private List<MockDriveEntry> driveEntryList = new ArrayList<>();

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private FirebaseUser mUser;
    private String mUserID;
    private String userEmail;


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
        if (mUser != null)
        {
            mUserID = mUser.getUid();
            userEmail = mUser.getEmail();
            mRef.child("user_"+DriveActivity.EmailHash(userEmail)).child("drive_entries").addChildEventListener(driveEntryListener);
        }


        // UI

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
        setTitle("Saved Drives");

        /* Initialize Drive Entries */
//        MockDriveEntry entry = new MockDriveEntry();
//        entry.setName("Evening Drive");
//        entry.setDateTime("May 15, 2019 at 2:30 PM");
//        entry.setDistance(18.7);
//        entry.setDuration("1 hour 23 min");
//        entry.setTopSpeed(87.8);
//        entry.setMap_thumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.dartmouth_map));
//        driveEntryList.add(entry);
//
//        entry = new MockDriveEntry();
//        entry.setName("Evening Drive");
//        entry.setDateTime("May 15, 2019 at 2:30 PM");
//        entry.setDistance(18.7);
//        entry.setDuration("1 hour 23 min");
//        entry.setTopSpeed(87.8);
//        entry.setMap_thumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.dartmouth_map));
//        driveEntryList.add(entry);
//
//        entry = new MockDriveEntry();
//        entry.setName("Evening Drive");
//        entry.setDateTime("May 15, 2019 at 2:30 PM");
//        entry.setDistance(18.7);
//        entry.setDuration("1 hour 23 min");
//        entry.setTopSpeed(87.8);
//        entry.setMap_thumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.dartmouth_map));
//        driveEntryList.add(entry);

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
                startActivity(intent);

            }
        }));

    }

    /**
     * Event listener for changes in Firebase exercise entries
     */
    ChildEventListener driveEntryListener = new ChildEventListener() {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            MockDriveEntry entry = snapshotToDriveEntry(dataSnapshot);
            driveEntryList.add(entry);

            mAdapter.notifyDataSetChanged();
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

    private MockDriveEntry snapshotToDriveEntry(DataSnapshot ds)
    {
        DriveEntry entry = new DriveEntry();

        entry.setDriveAvgSpeed((String) ds.child("summary").child("driveAvgSpeed").getValue());
        entry.setDriveDistance((String) ds.child("summary").child("driveDistance").getValue());
        entry.setDriveDuration((String) ds.child("summary").child("driveDuration").getValue());
        entry.setDriveName((String) ds.child("summary").child("driveName").getValue());
        entry.setDriveTimeStamp((String) ds.child("summary").child("driveTimeStamp").getValue());
        entry.setDriveTopSpeed((String) ds.child("summary").child("driveTopSpeed").getValue());
        entry.setLocationList((String) ds.child("summary").child("locationList").getValue());

        return new MockDriveEntry(entry, this);
    }

}
