package edu.dartmouth.cs.racetraq;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.DashPathEffect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

import java.util.ArrayList;

import edu.dartmouth.cs.racetraq.Models.DriveEntry;

public class DisplayDriveActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "DisplayDriveActivity";
    // Drive entry data
    private DriveEntry driveEntry;
    private String entryId;
    private ArrayList<Double> speedList;
    private ArrayList<Integer> engTempList;
    private ArrayList<Double> timeList;
    private String driveName;
    private long numPoints;
    private Bitmap bitmap;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mRef;
    private FirebaseUser mUser;
    private String mUserID;
    private String userEmail;
    private int pointCount = 0;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    // UI
    private XYPlot plot;
    private Intent launch_intent;
    private ImageView mapImageView;

    // Map
    private GoogleMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_drive);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        launch_intent = getIntent();

        byte[] byteArrayExtra = launch_intent.getByteArrayExtra(HistoryActivity.BYTE_ARRAY_KEY);
        bitmap = BitmapFactory.decodeByteArray(byteArrayExtra, 0, byteArrayExtra.length);

        // Firebase init

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();
        mUser = mAuth.getCurrentUser();
        entryId = launch_intent.getStringExtra(HistoryActivity.DRIVE_ENTRY_ID_KEY);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();


        if (mUser != null) {
            userEmail = mUser.getEmail();
            mUserID = "user_" + NewDriveActivity.EmailHash(userEmail);
            mRef.child(mUserID).child("drive_entries").child(entryId).child("datapoints").addChildEventListener(datapointListener);
        }

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

        mapImageView = findViewById(R.id.map_snapshot);
        mapImageView.setImageBitmap(bitmap);

        driveName = launch_intent.getStringExtra(HistoryActivity.DRIVE_NAME_KEY);
        setTitle(driveName);

        /* Set up plot */
        numPoints = launch_intent.getLongExtra(HistoryActivity.NUM_POINTS_KEY, 0);
        // initialize our XYPlot reference:
        plot = findViewById(R.id.speed_plot);

        timeList = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            timeList.add(i * 0.2);
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.display_map);
//        mapFragment.getMapAsync(this);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_display_drive_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.delete_drive_action) {
            // delete entry from Firebase database
            mRef.child(mUserID).child("drive_entries").child(entryId).removeValue();

            // delete map image from Firebase storage
            DeleteMapTask task = new DeleteMapTask();
            task.execute();

            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Event listener for changes in Firebase exercise entries
     */
    ChildEventListener datapointListener = new ChildEventListener() {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            String speedString = (String) dataSnapshot.child("speed").getValue();
            String engTempString = (String) dataSnapshot.child("eng_temp").getValue();

            if (speedList == null) {
                speedList = new ArrayList<>();
            }
            speedList.add(Double.parseDouble(speedString));

            if (engTempList == null) {
                engTempList = new ArrayList<>();
            }
            engTempList.add(Integer.parseInt(engTempString));

            pointCount++;

            if (pointCount == numPoints) {
                createPlot();
                plot.redraw();
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
        engTempSeriesFormat.getLinePaint().setPathEffect(new DashPathEffect(new float[]{

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

        // TODO: adjust legend positioning

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    /**
     * ASYNC TASKS
     **/

    class DeleteMapTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            // Create a reference to the file to delete
            StorageReference mapRef = storageReference.child(mUserID).child("mapSnapshots/" + entryId + ".jpeg");

            // Delete the file
            mapRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // File deleted successfully
                    Log.d(TAG, "Deleted map storage file");
//                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Uh-oh, an error occurred!
                    Log.d(TAG, "Failed to delete map storage file");
//                    finish();
                }
            });

            return null;
        }
    }
}
