package edu.dartmouth.cs.racetraq.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;

import java.util.Objects;

import edu.dartmouth.cs.racetraq.Models.DriveDatapoint;
import edu.dartmouth.cs.racetraq.NewDriveActivity;
import edu.dartmouth.cs.racetraq.R;


public class DashboardFragment extends Fragment {

    Context mActivity;
    private long startTime;

    // UI
    private TextView tpsTextView;
    private TextView engRpmTextView;
    private TextView speedTextView;
    private Chronometer mChronometer;
    private TextView engTempTextView;
    private TextView battVoltTextView;


    public DashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up UI
        if (mActivity != null)
        {
            tpsTextView = getActivity().findViewById(R.id.dash_tps);
            engRpmTextView = getActivity().findViewById(R.id.dash_eng_rpm);
            speedTextView = getActivity().findViewById(R.id.dash_speed);
            mChronometer = getActivity().findViewById(R.id.dash_timer);
            engTempTextView = getActivity().findViewById(R.id.dash_eng_temp);
            battVoltTextView = getActivity().findViewById(R.id.dash_batt_voltage);

        }

        startTime = ((NewDriveActivity) Objects.requireNonNull(getActivity())).getStartTime();
        long base = startTime - (System.currentTimeMillis() - SystemClock.elapsedRealtime());
        mChronometer.setBase(base);
        mChronometer.start();

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * PUBLIC METHODS
     */

    public void displayData(DriveDatapoint datapoint) {
        tpsTextView.setText(datapoint.getTps());
        engRpmTextView.setText(datapoint.getEng_rpm());
        speedTextView.setText(datapoint.getSpeed());
        engTempTextView.setText(datapoint.getEng_temp());
        battVoltTextView.setText(datapoint.getBatt_voltage());
    }

    public void startTime() {
        mChronometer.start();
    }

    public void stopTime() {
        mChronometer.stop();
    }

    /**
     * PRIVATE METHODS
     */

}
