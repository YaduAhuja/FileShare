package com.YAIndustries.fileshare.utilities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.YAIndustries.fileshare.models.FileMetaData;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public static void copyFile(AppCompatActivity activity, InputStream inputStream, OutputStream outputStream, ProgressBar progressBar, long fileSize) {
        int c, len = 1<<21;
        long count = 0;
        byte[] buf = new byte[len];
        try{
            while ((len = inputStream.read(buf)) != -1){
                outputStream.write(buf, 0, len);
                count += len;
                double progress = (count * 1.0) / fileSize * 100;
                activity.runOnUiThread(() -> progressBar.setProgress((int)progress));
            }
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

    @Nullable
    public static FileMetaData getFileMetaDataFromStream(@NonNull InputStream inputStream) {
        int c;
        var jsonBuilder = new StringBuilder();
        try {
            while ((c = inputStream.read()) != 0)
                jsonBuilder.append((char) c);

            var metaData = new ObjectMapper().readValue(jsonBuilder.toString(), FileMetaData.class);
            return metaData;
        } catch (Exception e) {
            Log.d("Parsing File Metadata From Stream", e.getMessage());
        }
        return null;
    }

    @Nullable
    public static FileMetaData getFileMetaDataFromCursor(@NonNull  Cursor cursor, @NonNull  int row) {
        // Query Result has Columns ["document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"]
        // And the Result starts before first row
        if(cursor.getCount() < row)
            return null;
        cursor.moveToPosition(row - 1);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        var ret = new FileMetaData();
        ret.name = cursor.getString(nameIndex);
        ret.size = cursor.getLong(sizeIndex);
        return ret;
    }

    @Nullable
    public static FileMetaData getFileMetaDataFromCursor(@NonNull  Cursor cursor) {
        if(cursor.getCount() < 1)
            return null;
        return getFileMetaDataFromCursor(cursor, 1);
    }
}
