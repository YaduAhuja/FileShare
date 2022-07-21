package com.yaindustries.fileshare.utilities;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.yaindustries.fileshare.models.FileMetaData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = "Utils";
    private static ObjectWriter jsonWriter;
    private static ObjectMapper objectMapper;

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showToast(Context context, String message, int duration) {
        Toast.makeText(context, message, duration).show();
    }

    public static boolean copyFile(AppCompatActivity activity, InputStream inputStream, OutputStream outputStream, ProgressBar progressBar, long fileSize) {
        // 8KB Buffer Size
        int len = 1 << 13;
        long count = 0;
        byte[] buf = new byte[len];
        boolean errorOccurred = false;
        try {
            while (count < fileSize) {
                len = (int) Math.min(fileSize - count, buf.length);
                inputStream.read(buf, 0, len);
                outputStream.write(buf, 0, len);
                count += len;
                double progress = (count * 1.0) / fileSize * 100;
                Log.d("Copy File", "Bytes Written : " + count);
                activity.runOnUiThread(() -> progressBar.setProgress((int) progress));
            }
            Log.d(TAG, "Done Copy " + count);
        } catch (IOException e) {
            Log.d("Copy File", e.getMessage());
            activity.runOnUiThread(() -> showToast(activity, "Error in Copy"));
            errorOccurred = true;
        }

        return !errorOccurred;
    }

    public static boolean checkVersionAboveQ() {
        return Build.VERSION_CODES.Q < Build.VERSION.SDK_INT;
    }

    public static boolean checkVersionAboveP() {
        return Build.VERSION_CODES.P < Build.VERSION.SDK_INT;
    }

    @Nullable
    public static FileMetaData getFileMetaDataFromStream(@NonNull InputStream inputStream) {
        int c;
        var jsonBuilder = new StringBuilder();
        try {
            while ((c = inputStream.read()) != 0)
                jsonBuilder.append((char) c);
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonBuilder.toString(), FileMetaData.class);
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
        Log.d(TAG, "getFileMetaDataFromCursor: {\nname: " + ret.name + " \nsize: " + ret.size + "\n}");
        return ret;
    }

    @Nullable
    public static FileMetaData getFileMetaDataFromCursor(@NonNull Cursor cursor) {
        if (cursor.getCount() < 1)
            return null;
        return getFileMetaDataFromCursor(cursor, 1);
    }

    public static FileMetaData getFileMetaDataFromUri(@NonNull Context context, @NonNull Uri fileUri) {
        var cursor = context.getContentResolver().query(fileUri, null, null, null, null);
        return getFileMetaDataFromCursor(cursor);
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

    public static boolean isLocationPermissionAvailable(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static String serializeFileMetaData(FileMetaData metaData) {
        if (jsonWriter == null)
            jsonWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
        String json;
        try {
            json = jsonWriter.writeValueAsString(metaData);
        } catch (JsonProcessingException e) {
            json = "";
        }
        return json;
    }
}
