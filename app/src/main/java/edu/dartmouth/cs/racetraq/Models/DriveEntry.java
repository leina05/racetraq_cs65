package edu.dartmouth.cs.racetraq.Models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class DriveEntry {
    private String driveTimeStamp;
    private String driveAvgSpeed;
    private String driveTopSpeed;
    private String driveDistance;

    public String getLocationList() {
        return locationList;
    }

    public void setLocationList(String locationList) {
        this.locationList = locationList;
    }

    private String locationList;
    private String driveName;

    public DriveEntry() {
    }

    public String getDriveTimeStamp() {
        return driveTimeStamp;
    }

    public void setDriveTimeStamp(String driveTimeStamp) {
        this.driveTimeStamp = driveTimeStamp;
    }

    public String getDriveAvgSpeed() {
        return driveAvgSpeed;
    }

    public void setDriveAvgSpeed(String driveAvgSpeed) {
        this.driveAvgSpeed = driveAvgSpeed;
    }

    public String getDriveTopSpeed() {
        return driveTopSpeed;
    }

    public void setDriveTopSpeed(String driveTopSpeed) {
        this.driveTopSpeed = driveTopSpeed;
    }

    public String getDriveDistance() {
        return driveDistance;
    }

    public void setDriveDistance(String driveDistance) {
        this.driveDistance = driveDistance;
    }

    public String getDriveName() {
        return driveName;
    }

    public void setDriveName(String driveName) {
        this.driveName = driveName;
    }
}
