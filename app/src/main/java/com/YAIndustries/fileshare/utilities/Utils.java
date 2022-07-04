package com.YAIndustries.fileshare.utilities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.YAIndustries.fileshare.models.FileMetaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final String TAG = "Utils";

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
        if (checkVersionAboveQ())
            return true;
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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

    public static boolean copyFile(AppCompatActivity activity, InputStream inputStream, OutputStream outputStream, ProgressBar progressBar, long fileSize) {
        // 1KB Buffer Size
        int len = 1 << 10;
        long count = 0;
        byte[] buf = new byte[len];
        boolean errorOccured = false;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
                count += len;
                double progress = (count * 1.0) / fileSize * 100;
                activity.runOnUiThread(() -> progressBar.setProgress((int) progress));
            }
        } catch (IOException e) {
            Log.d("Copy File", e.getMessage());
            activity.runOnUiThread(() -> showToast(activity, "Error in Copy"));
            errorOccured = true;
        }

        Log.d("Copy File", "Bytes Written : " + count);
        return !errorOccured;
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
    public static FileMetaData getFileMetaDataFromCursor(@NonNull Cursor cursor, @NonNull int row) {
        // Query Result has Columns ["document_id", "mime_type", "_display_name", "last_modified", "flags", "_size"]
        // And the Result starts before first row
        if (cursor.getCount() < row)
            return null;
        cursor.moveToPosition(row - 1);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
        var ret = new FileMetaData();
        ret.name = cursor.getString(nameIndex);
        ret.size = cursor.getLong(sizeIndex);
        String json = "";
        try {
            json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ret);
        } catch (JsonProcessingException e) {
            Log.d(TAG, "getFileMetaDataFromCursor: Error in Flushing JSON");
        }
        Log.d(TAG, "getFileMetaDataFromCursor: Parsed JSON\n" + json);
        return ret;
    }

    @Nullable
    public static FileMetaData getFileMetaDataFromCursor(@NonNull Cursor cursor) {
        if (cursor.getCount() < 1)
            return null;
        return getFileMetaDataFromCursor(cursor, 1);
    }

    // New API

    // Checks for read Storage and Write Storage Permissions
    private static boolean checkPermissionGranted(AppCompatActivity activity, String permission) {
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkStoragePermissions(AppCompatActivity activity) {
        if (checkVersionAboveQ())
            return true;

        return checkPermissionGranted(activity, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                checkPermissionGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static void requestStoragePermissions(AppCompatActivity activity) {
        List<String> permissions = new ArrayList<>(2);
        if (!checkPermissionGranted(activity, Manifest.permission.READ_EXTERNAL_STORAGE))
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        if (!checkPermissionGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (!permissions.isEmpty()) {
            var permissionsArray = permissions.toArray(new String[0]);
            activity.requestPermissions(permissionsArray, 12);
        }
    }

    public static FileMetaData getFileMetaDataFromUri(@NonNull Context context, @NonNull Uri fileUri) {
        var cursor = context.getContentResolver().query(fileUri, null, null, null, null);
        return getFileMetaDataFromCursor(cursor);
    }

    public static boolean ensureDataDirectory(AppCompatActivity activity) {
//        if (!checkStoragePermissions(activity)) {
//            Log.d(TAG, "ensureDataDirectory: Error in Fetching the Permissions");
//            return false;
//        }
        File f = new File(getDataDirectoryPath());
        return f.isDirectory() || f.mkdirs();
    }

    public static String getDataDirectoryPath() {
        return Environment.getExternalStorageDirectory() + "/Download/FileShare/";
    }

    public static byte[] getByteArrayFromInt(int num) {
        return new byte[]{
                (byte) (num),
                (byte) (num >> 8),
                (byte) (num >> 16),
                (byte) (num >> 24)
        };
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        return manufacturer + "-" + model;
    }
}
