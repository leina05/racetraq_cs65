package edu.dartmouth.cs.racetraq.Fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.Plot;
import com.androidplot.PlotListener;
import com.androidplot.util.PixelUtils;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Objects;

import edu.dartmouth.cs.racetraq.DriveActivity;
import edu.dartmouth.cs.racetraq.HistoryActivity;
import edu.dartmouth.cs.racetraq.NewDriveActivity;
import edu.dartmouth.cs.racetraq.R;


public class LiveGraphFragment extends Fragment {

    Context mActivity;
    private static final String TAG = "LiveGraphFragment";

    // Drive Entry Data
    private String entryId;
    private ArrayList<Double> speedList;
    private ArrayList<Double> timeList;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private FirebaseUser mUser;
    private String mUserID;
    private String userEmail;
    private int pointCount = 0;
    private long lastTimeStamp = 0;

    // UI
    private XYPlot plot;
    private Intent launch_intent;
    private SimpleXYSeries speedSeries;
    private Redrawer redrawer;


    public LiveGraphFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase init
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();
        mUser = mAuth.getCurrentUser();

        entryId = Long.toString(((NewDriveActivity) mActivity).getStartTime());

        if (mUser != null)
        {
            userEmail = mUser.getEmail();
            mUserID = "user_"+ DriveActivity.EmailHash(userEmail);
            mRef.child(mUserID).child("drive_entries").child(entryId).child("datapoints").addChildEventListener(datapointListener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_live_graph, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated");

        if (timeList == null)
        {
            timeList = new ArrayList<>();
        }
        if (speedList == null)
        {
            speedList = new ArrayList<>();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        redrawer.pause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        createPlot();

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /** FIREBASE **/

    /**
     * Event listener for changes in Firebase exercise entries
     */
    ChildEventListener datapointListener = new ChildEventListener() {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            String speedString = (String) dataSnapshot.child("speed").getValue();
            Double speed = Double.parseDouble(speedString);
            Double time = 0.2*pointCount;
            long timestamp = Long.parseLong((String) dataSnapshot.child("timestamp").getValue());

            if (timestamp > lastTimeStamp)
            {
                if (speedList == null)
                {
                    speedList = new ArrayList<>();
                }
                speedList.add(speed);

                if (timeList == null)
                {
                    timeList = new ArrayList<>();
                }
                timeList.add(time);

                speedSeries.addLast(time, speed);

                lastTimeStamp = timestamp;
                pointCount++;
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

    /** PRIVATE FUNCTIONS **/

    private void createPlot() {

        // plot init
        plot = getActivity().findViewById(R.id.live_plot);

        redrawer = new Redrawer(plot, 30, false);

        // turn the above arrays into XYSeries':
        if (speedSeries == null)
        {
            speedSeries = new SimpleXYSeries(
                    timeList, speedList, "Speed");
        }

        // create formatters to use for drawing a series using LineAndPointRenderer
        // and configure them from xml:
        LineAndPointFormatter speedSeriesFormat =
                new LineAndPointFormatter(mActivity, R.xml.line_point_formatter_with_labels);

        // add a new series' to the xyplot:
        plot.addSeries(speedSeries, speedSeriesFormat);

        plot.setDomainBoundaries(null, null, BoundaryMode.AUTO);
        plot.setRangeBoundaries(null, null, BoundaryMode.AUTO);
//        plot.setRenderMode(Plot.RenderMode.USE_BACKGROUND_THREAD);

        redrawer.start();


    }

}
