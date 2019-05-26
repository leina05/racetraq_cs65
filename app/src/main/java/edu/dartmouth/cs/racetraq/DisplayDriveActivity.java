package edu.dartmouth.cs.racetraq;

import android.content.Intent;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.androidplot.ui.DynamicTableModel;
import com.androidplot.ui.FixedTableModel;
import com.androidplot.ui.Size;
import com.androidplot.ui.SizeMode;
import com.androidplot.ui.TableOrder;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;

import edu.dartmouth.cs.racetraq.Models.MockDriveEntry;

public class DisplayDriveActivity extends AppCompatActivity {

    // Drive entry data
    private MockDriveEntry driveEntry;
    private String entryId;
    private ArrayList<Double> speedList;
    private ArrayList<Integer> engTempList;
    private ArrayList<Double> timeList;
    private String driveName;
    private long numPoints;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private FirebaseUser mUser;
    private String mUserID;
    private String userEmail;
    private int pointCount = 0;

    // UI
    private XYPlot plot;

    private Intent launch_intent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_drive);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        launch_intent = getIntent();

        // Firebase init

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();
        mUser = mAuth.getCurrentUser();
        entryId = launch_intent.getStringExtra(HistoryActivity.DRIVE_ENTRY_ID_KEY);

        if (mUser != null)
        {
            userEmail = mUser.getEmail();
            mUserID = "user_"+DriveActivity.EmailHash(userEmail);
            mRef.child(mUserID).child("drive_entries").child(entryId).child("datapoints").addChildEventListener(datapointListener);
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

        driveName = launch_intent.getStringExtra(HistoryActivity.DRIVE_NAME_KEY);
        setTitle(driveName);

        /* Set up plot */
        numPoints = launch_intent.getLongExtra(HistoryActivity.NUM_POINTS_KEY, 0);
        // initialize our XYPlot reference:
        plot = findViewById(R.id.speed_plot);

        timeList = new ArrayList<>();
        for (int i = 0; i < numPoints; i++)
        {
            timeList.add(i*0.2);
        }


    }

    /**
     * Event listener for changes in Firebase exercise entries
     */
    ChildEventListener datapointListener = new ChildEventListener() {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            String speedString = (String) dataSnapshot.child("speed").getValue();
            String engTempString = (String) dataSnapshot.child("eng_temp").getValue();

            if (speedList == null)
            {
                speedList = new ArrayList<>();
            }
            speedList.add(Double.parseDouble(speedString));

            if (engTempList == null)
            {
                engTempList = new ArrayList<>();
            }
            engTempList.add(Integer.parseInt(engTempString));

            pointCount++;

            if (pointCount == numPoints)
            {
                createPlot();
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

    private void createPlot() {
        // turn the above arrays into XYSeries':
        // (Y_VALS_ONLY means use the element index as the x value)
        XYSeries speedSeries = new SimpleXYSeries(
                timeList, speedList, "Speed");

        XYSeries engTempSeries = new SimpleXYSeries(
                timeList, engTempList, "Engine Temp");

        // create formatters to use for drawing a series using LineAndPointRenderer
        // and configure them from xml:
        LineAndPointFormatter speedSeriesFormat =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels);

        LineAndPointFormatter engTempSeriesFormat =
                new LineAndPointFormatter(this, R.xml.line_point_formatter_with_labels_2);

        // add an "dash" effect to the series2 line:
        engTempSeriesFormat.getLinePaint().setPathEffect(new DashPathEffect(new float[] {

        // always use DP when specifying pixel sizes, to keep things consistent across devices:
        PixelUtils.dpToPix(20),
        PixelUtils.dpToPix(15)}, 0));

        // just for fun, add some smoothing to the lines:
        // see: http://androidplot.com/smooth-curves-and-androidplot/
        speedSeriesFormat.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        engTempSeriesFormat.setInterpolationParams(
                new CatmullRomInterpolator.Params(10, CatmullRomInterpolator.Type.Centripetal));

        // add a new series' to the xyplot:
        plot.addSeries(speedSeries, speedSeriesFormat);
        plot.addSeries(engTempSeries, engTempSeriesFormat);

        plot.setDomainBoundaries(null, null, BoundaryMode.AUTO);
        plot.setRangeBoundaries(null, null, BoundaryMode.AUTO);

//        plot.getLegend().setTableModel(new FixedTableModel(PixelUtils.dpToPix(300),
//                PixelUtils.dpToPix(100), TableOrder.ROW_MAJOR));

        plot.getLegend().setSize(new Size(
                PixelUtils.dpToPix(40), SizeMode.ABSOLUTE,
                PixelUtils.dpToPix(225), SizeMode.ABSOLUTE));

        plot.getLegend().setTableModel(new DynamicTableModel(2, 1, TableOrder.ROW_MAJOR));


    }

}
