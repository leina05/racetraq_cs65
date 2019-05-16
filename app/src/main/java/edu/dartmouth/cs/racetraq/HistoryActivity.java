package edu.dartmouth.cs.racetraq;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import edu.dartmouth.cs.racetraq.Adapters.HistoryViewAdapter;
import edu.dartmouth.cs.racetraq.Models.DriveEntry;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView.Adapter mAdapter;
    private List<DriveEntry> driveEntryList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
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
        setTitle("Saved Drives");

        /* Initialize Drive Entries */
        DriveEntry entry = new DriveEntry();
        entry.setName("Evening Drive");
        entry.setDateTime("May 15, 2019 at 2:30 PM");
        entry.setDistance(18.7);
        entry.setDuration("1 hour 23 min");
        entry.setTopSpeed(87.8);
        entry.setMap_thumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.dartmouth_map));
        driveEntryList.add(entry);

        entry = new DriveEntry();
        entry.setName("Evening Drive");
        entry.setDateTime("May 15, 2019 at 2:30 PM");
        entry.setDistance(18.7);
        entry.setDuration("1 hour 23 min");
        entry.setTopSpeed(87.8);
        entry.setMap_thumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.dartmouth_map));
        driveEntryList.add(entry);

        entry = new DriveEntry();
        entry.setName("Evening Drive");
        entry.setDateTime("May 15, 2019 at 2:30 PM");
        entry.setDistance(18.7);
        entry.setDuration("1 hour 23 min");
        entry.setTopSpeed(87.8);
        entry.setMap_thumbnail(BitmapFactory.decodeResource(getResources(), R.drawable.dartmouth_map));
        driveEntryList.add(entry);

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

    }

}
