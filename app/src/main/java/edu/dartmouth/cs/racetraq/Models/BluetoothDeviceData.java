package edu.dartmouth.cs.racetraq.Models;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.UUID;

public class BluetoothDeviceData {
    private BluetoothDevice device;
    private int rssi;
    private byte[] scanRecord;
    private String advertisedName;           // Advertised name
    private String cachedNiceName;
    private String cachedName;

    // Decoded scan record (update R.array.scan_devicetypes if this list is modified)
    public static final int kType_Unknown = 0;
    public static final int kType_Uart = 1;
    public static final int kType_Beacon = 2;
    public static final int kType_UriBeacon = 3;

    public int type;
    private int txPower;
    private ArrayList<UUID> uuids;

    public String getName() {
        if (cachedName == null) {
            cachedName = device.getName();
            if (cachedName == null) {
                cachedName = advertisedName;      // Try to get a name (but it seems that if device.getName() is null, this is also null)
            }
        }

        return cachedName;
    }

    public String getNiceName() {
        if (cachedNiceName == null) {
            cachedNiceName = getName();
            if (cachedNiceName == null) {
                cachedNiceName = device.getAddress();
            }
        }

        return cachedNiceName;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(byte[] scanRecord) {
        this.scanRecord = scanRecord;
    }

    public void setAdvertisedName(String advertisedName) {
        this.advertisedName = advertisedName;
    }

    public int getTxPower() {
        return txPower;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public ArrayList<UUID> getUuids() {
        return uuids;
    }

    public void setUuids(ArrayList<UUID> uuids) {
        this.uuids = uuids;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public BluetoothDevice getDevice()  {
        return device;
    }
}
