package edu.dartmouth.cs.racetraq.Models;

public class HomePageStats {

    private String savedDrives;
    private String milesDriven;
    private String topSpeed;

    public HomePageStats() {
    }

    public HomePageStats(int savedDrives, double milesDriven, double topSpeed)
    {
        this.savedDrives = Integer.toString(savedDrives);
        this.milesDriven = Double.toString(milesDriven);
        this.topSpeed = Double.toString(topSpeed);
    }


    public String getSavedDrives() {
        return savedDrives;
    }

    public void setSavedDrives(String savedDrives) {
        this.savedDrives = savedDrives;
    }

    public String getMilesDriven() {
        return milesDriven;
    }

    public void setMilesDriven(String milesDriven) {
        this.milesDriven = milesDriven;
    }

    public String getTopSpeed() {
        return topSpeed;
    }

    public void setTopSpeed(String topSpeed) {
        this.topSpeed = topSpeed;
    }
}
