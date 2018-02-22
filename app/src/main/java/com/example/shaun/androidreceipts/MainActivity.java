package com.example.shaun.androidreceipts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.example.shaun.androidreceipts.R.id.activity_main;
import static com.example.shaun.androidreceipts.R.id.image;
import static com.example.shaun.androidreceipts.R.id.mImageView;
import static com.example.shaun.androidreceipts.R.id.payee;

/** This app is designed to take photographs (of receipts) and name them conveniently
 * describing the:
 * 1) Merchant
 * 2) Date (YYYYMMDD)
 * 3) amount
 * 4) optionally, whether the receipt is a refund.
 *
 * The picture file is then automatically passed to a PC via Bluetooth transfer
 */
public class MainActivity extends AppCompatActivity {
    String mCurrentPhotoPath;
    private static final String TAG = MainActivity.class.getSimpleName();
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_ENABLE_BT = 2;
    //Get the bluetooth adapter so it is accessible throughout the class.
    private BluetoothAdapter mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
    private String myLaptop = "";
    File image;
    /* TODO - listen for the ACTION_STATE_CHANGED broadcast intent so that the app can detect
     *          when the BT status changes and can react to it.
     * TODO - work out when the blue tooth discovery scan has completed and restarted.  Once this happens there is
     *          no point carrying on consuming resources.  Display an error and stop discovery.
     *          Implement a button to re-attempt connection in the case where the remote device cant be found.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //now check for bluetooth adaptor
        //BluetoothAdapter mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBlueToothAdapter == null){
            //bluetooth not supported - report an error and exit app
            super.onDestroy();
        }
        Log.d(TAG, "MY DEBUG: About to check if bluetooth is enabled");
        if (!mBlueToothAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
        //Register for broadcasts when a device is connected
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver,filter);
        if (mBlueToothAdapter.isDiscovering()){
            mBlueToothAdapter.cancelDiscovery();
        }
        //mBlueToothAdapter.startDiscovery();

        //clean up the photo directory before the app launches
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        DeleteRecursive(storageDir.toString());

        //Set default date value of today in the date field.
        EditText myDate = (EditText) findViewById(R.id.receiptDate);
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        myDate.setText(timeStamp);
    }


    public void DeleteRecursive(String strPath) {

        File fileOrDirectory = new File(strPath);

        if (fileOrDirectory.isDirectory()){
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child.getPath());
            fileOrDirectory.delete();
        }else{
            fileOrDirectory.delete();
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                //Discovery has found a BT device.  Get the BT device object and info
                //from its intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                Log.d(TAG, "New BlueTooth Device found by the app:\n Name: " + deviceName + " Address: " + deviceHardwareAddress);
                if (deviceName == null) {
                    Log.d(TAG,"Wow!!! a device broadcasting bluetooth with no name");
                }
                else {
                    if (deviceName.equals("LAPTOP-T4E2TN63")) {
                        mBlueToothAdapter.cancelDiscovery();
                    }
                }
            }
        }
    };
    public void transmitFile(View view){
        //if this method has been called then the laptop MAC address has been obtained and passed
        //We need to connect and open an RFCOMM channel which we can keep open
        Intent intent = new Intent();
        intent.setAction(intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(intent.EXTRA_STREAM, Uri.fromFile(image));

        PackageManager pm = getPackageManager();
        List<ResolveInfo> appsList = pm.queryIntentActivities(intent, 0);
        if (appsList.size()>0) {
            String packageName = null;
            String className = null;
            boolean found = false;

            for (ResolveInfo info: appsList){
                packageName = info.activityInfo.packageName;
                if (packageName.equals("com.android.bluetooth")){
                    className = info.activityInfo.name;
                    found = true;
                    break;
                }
            }
            if (!found){
                Toast.makeText(this, "BT intent not found on device", Toast.LENGTH_SHORT).show();
            }
            intent.setClassName(packageName, className);
            startActivity(intent);
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"In method On activity result 2");
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d(TAG, "Image captured...");
            //Since modifying the code to use the URI instead of the GIF thumbnail, the data object from the intent is always null
            //modified the code to use the globally stored URI string and the Uri.parse function to load into the ImageView object.
            //Bundle extras = data.getExtras();
            //Bundle extras = getIntent().getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            ImageView mImageView = (ImageView) findViewById(R.id.mImageView);
            //mImageView.setImageBitmap(imageBitmap);
            Uri photoURI = Uri.parse(mCurrentPhotoPath);
            mImageView.setImageDrawable(null);
            mImageView.setImageURI(photoURI);
            TextView lbl = (TextView) findViewById(R.id.myFile);
            lbl.setText(mCurrentPhotoPath);
        }

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            //You should only get here if BT was off and now it is on.  what about the case where it was already on?
            Log.d(TAG, "Shaun says BlueTooth is now enabled, and app knows about it - Happy days!!!");
            //Now we need to see if the paired device is already known
            Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            if (pairedDevices.size() > 0){
                Log.d(TAG,"All known blue tooth devices are:");
                for (BluetoothDevice device:pairedDevices){
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    Log.d(TAG, "Name: " + deviceName + " Address: " + deviceHardwareAddress);
                    if (deviceName.equals("LAPTOP-T4E2TN63")){
                        mBlueToothAdapter.cancelDiscovery();
                        myLaptop = deviceName;
                        break;
                    }
                }
            }
            if (myLaptop.isEmpty()){
                mBlueToothAdapter.startDiscovery();
                Log.d(TAG, "starting Discovery process");
            }
            //mBlueToothAdapter.startDiscovery();
        }
        if (requestCode==REQUEST_ENABLE_BT && resultCode==RESULT_CANCELED){
            Log.d(TAG,"Seems that the user did not turn on Blue Tooth, or the devices BT adaptor did not turn on properly");
        }
    }

    /*Done - need to work out how to define method takePicture for the button onClick action
    * it turns out you dont specify the parenthesis in the call to the method.  it's as simple as that; otherwise
    * it attempts to run "takePicture()()" and can't find it.  You must also provide a view attribute in the definition
    * otherwise you get a runtime error when the method is called.*/
    public void takePicture(View view) {

        dispatchTakePictureIntent();
    }

    public void dispatchTakePictureIntent() {
         Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
         if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
             // Create the File where the photo should go
             File photoFile = null;
             try {
                 photoFile = createImageFile();
                 } catch (IOException ex) {
                 // Error occurred while creating the File
                    Log.e(TAG, "Coudl not create space in android file system to store image");
                 }
             // Continue only if the File was successfully created
             if (photoFile != null) {
                 Uri photoURI = FileProvider.getUriForFile(this,
                         "com.example.android.fileprovider",
                         photoFile);
                 takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                 startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                 Log.d(TAG, "requesting photo intent");
             }
         }
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
        EditText myPayer = (EditText) findViewById(R.id.payee);
        EditText myDate = (EditText) findViewById(R.id.receiptDate);
        EditText myAmount = (EditText) findViewById(R.id.amount);
        CheckBox myRefund = (CheckBox) findViewById(R.id.refund);
        String imageFileName = myPayer.getText() + "_" + myDate.getText() + "_" + myAmount.getText();

        if (myRefund.isChecked()){
            imageFileName = imageFileName + "_Refund";
        }

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //image = File.createTempFile(
        //        imageFileName,  /* prefix */
        //       ".jpg",         /* suffix */
        //        storageDir      /* directory */
        //       );
        image = new File(storageDir,imageFileName + ".jpg");
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "closing application now - Adios!!!!!");
        unregisterReceiver(mReceiver);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        Log.d(TAG, "Configuration changed");
        super .onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            Log.d(TAG, "Now Lanscape orientation");
        else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "Now Portrait orientation");
            ImageView mImageView = (ImageView) findViewById(R.id.mImageView);
            //mImageView.setImageBitmap(imageBitmap);
            Uri photoURI = Uri.parse(mCurrentPhotoPath);
            mImageView.setImageDrawable(null);
            mImageView.setImageURI(photoURI);
            TextView lbl = (TextView) findViewById(R.id.myFile);
            lbl.setText(mCurrentPhotoPath);
        }
        else
            Log.w(TAG, "unknown orientation" + orientation);
    }
}

    /*    public void reConnect(View view) {
        if (mBlueToothAdapter.isDiscovering()) {
            Log.d(TAG, "already discovering, why are you trying to rediscover. Must be something wrong.  Stopping discovery");
            mBlueToothAdapter.cancelDiscovery();
        }
        else{
            mBlueToothAdapter.startDiscovery();
        }
    }
    */
