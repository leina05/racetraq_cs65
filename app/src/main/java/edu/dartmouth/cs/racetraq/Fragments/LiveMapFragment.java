package edu.dartmouth.cs.racetraq.Fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import edu.dartmouth.cs.racetraq.R;
import edu.dartmouth.cs.racetraq.Services.TrackingService;


public class LiveMapFragment extends Fragment {

    private static final int PERMISSION_REQUEST_CODE = 1;


    private Context mActivity;

    // UI
    MapView mMapView;
    private GoogleMap googleMap;

    private Marker startMarker = null;
    private Marker pathMarker = null;
    private Polyline path;
    private PolylineOptions pathOptions;

    public LiveMapFragment() {
        // Required empty public constructor
    }

    /**
     * OVERRIDE METHODS
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_live_map, container, false);

        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                if (!checkPermission())
                    ActivityCompat.requestPermissions((Activity) mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
                else
                    setupMap();
            }
        });

        return rootView;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    /**
     * PRIVATE METHODS
     */

    /**
     * Check run-time permissions
     */
    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    private void setupMap()
    {

    }

    /** PUBLIC METHODS **/

    public void updateDisplay(Location location) {

        if (location != null) {
            // Update the map location
            LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17));

            // add startMarker if needed
            if (startMarker == null)
            {
                startMarker = googleMap.addMarker(new MarkerOptions().position(latlng).icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_GREEN)));

                pathOptions = new PolylineOptions().add(latlng);
                pathOptions.color(Color.BLACK);

            }
            else
            {
                // update pathMarker
                if (pathMarker == null)
                    pathMarker = googleMap.addMarker(new MarkerOptions().position(latlng).icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED)));
                else
                    pathMarker.setPosition(latlng);

                // update path
                if (path != null)
                    path.remove();

                pathOptions.add(latlng);
                path = googleMap.addPolyline(pathOptions);

                // update entry parameters
            }
        }
    }


}
