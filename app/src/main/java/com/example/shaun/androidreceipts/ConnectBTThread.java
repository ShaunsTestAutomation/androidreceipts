package com.example.shaun.androidreceipts;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by shaun on 30/11/2017.
 */

public class ConnectBTThread extends Thread{
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final UUID MY_UUID = UUID.randomUUID();
    private static final String TAG = ConnectBTThread.class.getSimpleName();

    public ConnectBTThread(BluetoothDevice device){
        BluetoothSocket tmpSocket = null;
        mmDevice = device;

        try {
            tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e){
            Log.e(TAG, "Sockets create method failed", e);
        }
        mmSocket = tmpSocket;
    }

    public void run(){
        try {
            mmSocket.connect();
        } catch (IOException connectException){
            try {
                mmSocket.close();
            } catch (IOException closeException){
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

    }
}
