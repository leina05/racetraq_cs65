package edu.dartmouth.cs.racetraq.Models;

import android.graphics.Bitmap;

public class DriveEntry {
    private String name;
    private String dateTime;
    private double distance;
    private double topSpeed;
    private String duration;
    private Bitmap map_thumbnail;


    public DriveEntry() {

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
}
