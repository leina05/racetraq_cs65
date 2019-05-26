package edu.dartmouth.cs.racetraq.Models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import edu.dartmouth.cs.racetraq.R;

public class MockDriveEntry {
    private String name;
    private String dateTime;
    private long timeMillis;
    private double distance;
    private double topSpeed;
    private double avgSpeed;
    private String duration;
    private ArrayList<LatLng> locationList;
    private Bitmap map_thumbnail;
    private long numPoints;

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm", Locale.US);



    public MockDriveEntry() {

    }

    public MockDriveEntry(DriveEntry driveEntry, Context context)
    {
        this.name = driveEntry.getDriveName();
        this.dateTime = driveEntry.getDriveTimeStamp();
        this.timeMillis = 0;

        this.avgSpeed = Double.parseDouble(driveEntry.getDriveAvgSpeed());
        this.topSpeed = Double.parseDouble(driveEntry.getDriveTopSpeed());
        this.distance = Double.parseDouble(driveEntry.getDriveDistance());
        this.duration = driveEntry.getDriveDuration();

        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<LatLng>>(){}.getType();
        this.locationList = gson.fromJson(driveEntry.getLocationList(), collectionType);

        this.numPoints = Long.parseLong(driveEntry.getNumPoints());

        // Set dummy bitmap for now
        this.map_thumbnail = BitmapFactory.decodeResource(context.getResources(), R.drawable.dartmouth_map);

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getTopSpeed() {
        return topSpeed;
    }

    public void setTopSpeed(double topSpeed) {
        this.topSpeed = topSpeed;
    }


    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Bitmap getMap_thumbnail() {
        return map_thumbnail;
    }

    public void setMap_thumbnail(Bitmap map_thumbnail) {
        this.map_thumbnail = map_thumbnail;
    }

    public ArrayList<LatLng> getLocationList() {
        return locationList;
    }

    public void setLocationList(ArrayList<LatLng> locationList) {
        this.locationList = locationList;
    }

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }

    public long getNumPoints() {
        return numPoints;
    }

    public void setNumPoints(long numPoints) {
        this.numPoints = numPoints;
    }
}
