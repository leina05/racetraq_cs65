package edu.dartmouth.cs.racetraq.Models;

public class HomePageStats {

    private int savedDrives;
    private long milesDriven;
    private long topSpeed;

    public HomePageStats() {
    }

    public int getSavedDrives() {
        return savedDrives;
    }

    public void setSavedDrives(int savedDrives) {
        this.savedDrives = savedDrives;
    }

    public long getMilesDriven() {
        return milesDriven;
    }

    public void setMilesDriven(long milesDriven) {
        this.milesDriven = milesDriven;
    }

    public long getTopSpeed() {
        return topSpeed;
    }

    public void setTopSpeed(long topSpeed) {
        this.topSpeed = topSpeed;
    }
}
