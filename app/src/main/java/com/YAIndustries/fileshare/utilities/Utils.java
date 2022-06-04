package com.YAIndustries.fileshare.utilities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    public static void showToast(Context context, String message, int duration) {
        Toast.makeText(context, message, duration).show();
    }

    public static boolean checkLocationPermission(AppCompatActivity activity) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 12);

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showToast(activity, "Permission Not Granted");
                Log.d("Permission Request", "Not Granted");
                return false;
            }
        } else
            Log.d("Permission Request", "Granted");
        return true;
    }

    public static boolean checkWriteStoragePermission(AppCompatActivity activity) {
        if(checkVersionAboveQ())
            return true;
        if(ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 12);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showToast(activity, "Permission Not Granted");
                Log.d("Permission Request", "Not Granted");
                return false;
            }
        } else
            Log.d("Permission Request", "Granted");
        return true;
    }

    public static boolean checkReadStoragePermission(AppCompatActivity activity) {
        if(ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 12);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showToast(activity, "Permission Not Granted");
                Log.d("Permission Request", "Not Granted");
                return false;
            }
        } else
            Log.d("Permission Request", "Granted");
        return true;
    }

    public static void copyFile(AppCompatActivity activity, InputStream inputStream, OutputStream outputStream) {
        int c, len = 1<<18;
        long count = 0;
        byte[] buf = new byte[len];
        try{
            while ((len = inputStream.read(buf)) != -1){
                outputStream.write(buf, 0, len);
                count += len;
            }
            inputStream.close();
            outputStream.close();
        }catch (IOException e){
            Log.d("Copy File", e.getMessage());
            activity.runOnUiThread(()-> showToast(activity, "Error in Copy"));
        }
        Log.d("Copy File", "Bytes Written : "+ count);
        final long countCopy = count;
        activity.runOnUiThread(() -> showToast(activity, "Bytes Written : "+ countCopy));
    }

    public static boolean checkVersionAboveQ() {
        return Build.VERSION_CODES.Q < Build.VERSION.SDK_INT;
    }
}
