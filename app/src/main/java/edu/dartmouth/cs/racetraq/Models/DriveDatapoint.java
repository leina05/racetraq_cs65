package edu.dartmouth.cs.racetraq.Models;

public class DriveDatapoint {
    private String timestamp;
    private String tps;
    private String eng_rpm;
    private String speed;
    private String eng_temp;
    private String batt_voltage;

    public DriveDatapoint(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTps() {
        return tps;
    }

    public void setTps(String tps) {
        this.tps = tps;
    }

    public String getEng_rpm() {
        return eng_rpm;
    }

    public void setEng_rpm(String eng_rpm) {
        this.eng_rpm = eng_rpm;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getEng_temp() {
        return eng_temp;
    }

    public void setEng_temp(String eng_temp) {
        this.eng_temp = eng_temp;
    }

    public String getBatt_voltage() {
        return batt_voltage;
    }

    public void setBatt_voltage(String batt_voltage) {
        this.batt_voltage = batt_voltage;
    }
}
