package com.example.shaun.androidreceipts;

import android.content.Intent;
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

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_STREAM;


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
    File image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    public void transmitFile(View view){
        //if this method has been called then the laptop MAC address has been obtained and passed
        //We need to connect and open an RFCOMM channel which we can keep open
        Intent intent = new Intent();
        intent.setAction(ACTION_SEND);
        intent.setType("*/*");
        /* In Android version 24 this line of code throws an exception as it assumes the remote target
        will have access to the URI.  This is no longer valid and better coding practice is now enforced.
        Remove this line of code
         */
        //intent.putExtra(EXTRA_STREAM, Uri.fromFile(image));
        /* and replace with */
        Uri apkURI = FileProvider.getUriForFile(getApplicationContext(), "com.example.android.fileprovider", image );
        intent.putExtra(EXTRA_STREAM, apkURI);
        intent.addFlags(intent.FLAG_GRANT_READ_URI_PERMISSION);

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
                Log.d(TAG,"Bluetooth is not supported on the device, about to pop-up toast");
                Toast.makeText(this, "BT intent not found on device", Toast.LENGTH_SHORT).show();
            }
            else {
                intent.setClassName(packageName, className);
                startActivity(intent);
            }
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
                    Log.e(TAG, "Could not create space in android file system to store image");
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
