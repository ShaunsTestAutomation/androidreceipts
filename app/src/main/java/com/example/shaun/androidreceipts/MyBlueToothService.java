package com.example.shaun.androidreceipts;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.util.logging.Handler;

/**
 * Created by shaun on 29/11/2017.
 * based upon the example from
 * https://developer.android.com/guide/topics/connectivity/bluetooth.html
 */

public class MyBlueToothService {
    private static final String TAG = MyBlueToothService.class.getSimpleName();
    private Handler mHandler;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

    }
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;    //mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Get the stream as temps since member streams are final
            try{
                tmpIn = socket.getInputStream();
            } catch (IOException e){
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try{
                tmpOut = socket.getOutputStream();
            } catch (IOException e){
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; //number of bytes returned from read()

            while (true){
                try {
                    //Read from input stream
                    numBytes = mmInStream.read(mmBuffer);
                    //Send bytes to the UI activity
                    Message readMSG = mHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer);
                    readMSG.sendToTarget();
                } catch (IOException e){
                    Log.e(TAG,"Input stream was disconnected!!!");
                    break;
                }
            }
        }

        public void write(byte[] bytes){
            try {
                mmOutStream.write(bytes);
                //Send the message to UI
                Message writtenMSG = mHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE, -1,-1, mmBuffer);
                writtenMSG.sendToTarget();
            } catch (IOException e){
                Log.e(TAG, "Error occurred when sending data!!!", e);

                //Report failure back to UI activity
                Message writeErrorMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        public void cancel() {
            try{
                mmSocket.close();
            } catch (IOException e){
                Log.e(TAG, "Could not close the connected socket", e);
            }
        }
    }
}
