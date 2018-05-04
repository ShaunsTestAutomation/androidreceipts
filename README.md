# androidreceipts
Android project that captures receipt information for bluetooth transfer

The project is a simple android app that asks the user for:
payee
date
amount
refund (Y/N)

It calls the ACTION_IMAGE_CAPTURE intent to use the onboard camera to take a picture (assumes a receipt).
The picture is displayed to the user, and given a filename consisting of the captured data (<payee>_<date>_<amount>[_<refund>].jpg)
The user can then choose to transfer the image file to another device via bluetooth.

The file transfer is performed with the use of the intent from the package "com.android.bluetooth"

Each time the app is started (not resumed), i.e. onCreate() all cached images from the previous use are deleted so as not to consume too much storage resource.

#Issues
There is nothing in the way of error handling implemented.  It works for a very specific purpose.  The code needs to be made more robust.
