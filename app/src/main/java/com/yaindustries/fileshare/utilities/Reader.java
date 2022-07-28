package com.yaindustries.fileshare.utilities;

import android.util.Log;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.yaindustries.fileshare.models.FileMetaData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Reader {
    //4MB Buffer Length for Reader
    private static final int BUF_LEN = 1 << 22;
    private static final byte[] BUF = new byte[BUF_LEN];
    private static final String TAG = "Reader";
    private static ObjectMapper objectMapper;
    private static ObjectWriter objectWriter;

    //Read Methods
    public static FileMetaData readFileMetaDataFromNetworkStream(DataInputStream is) throws IOException {
        var jsonStream = new ByteArrayOutputStream();
        var jsonDataStream = new DataOutputStream(jsonStream);
        readBlockFromNetworkStream(is, jsonDataStream);
        var json = jsonStream.toString("UTF-8");
        if (objectMapper == null)
            objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, FileMetaData.class);
    }

    public static void readBlockFromNetworkStream(DataInputStream is, DataOutputStream os) throws IOException {
        readBlockFromNetworkStream(is, os, null, null);
    }

    public static void readBlockFromNetworkStream(DataInputStream is, DataOutputStream os, AppCompatActivity activity, ProgressBar progressBar) throws IOException {
        long size = is.readLong();
        transferBytes(is, os, size, activity, progressBar);
    }

    //Write Methods
    public static void writeFileMetaDataToNetworkStream(FileMetaData metaData, DataOutputStream os) throws IOException {
        if (objectWriter == null) {
            if (objectMapper == null)
                objectMapper = new ObjectMapper();
            objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
        }
        var json = objectWriter.writeValueAsString(metaData);
        var jsonBytes = json.getBytes("UTF-8");
        var jsonStream = new DataInputStream(new ByteArrayInputStream(jsonBytes));

        writeBlockToNetworkStream(jsonStream, os, jsonBytes.length);
    }

    public static void writeBlockToNetworkStream(DataInputStream is, DataOutputStream os, long size) throws IOException {
        writeBlockToNetworkStream(is, os, size, null, null);
    }

    public static void writeBlockToNetworkStream(DataInputStream is, DataOutputStream os, long size, AppCompatActivity activity, ProgressBar progressBar) throws IOException {
        os.writeLong(size);
        transferBytes(is, os, size, activity, progressBar);
    }

    //Utility Methods
    private static void transferBytes(DataInputStream is, DataOutputStream os, long size, AppCompatActivity activity, ProgressBar progressBar) throws IOException {
        long count = 0;
        int len;
        while (count < size) {
            len = (int) Math.min(BUF_LEN, size - count);
            len = is.read(BUF, 0, len);
            os.write(BUF, 0, len);
            count += len;
            int progress = (int) (((count * 1.0) / size) * 100);
            Log.d(TAG, "transferBytes: " + progress);
            notifyProgress(activity, progressBar, progress);
        }
    }

    private static void notifyProgress(AppCompatActivity activity, ProgressBar progressBar, int progress) {
        if (activity == null || progressBar == null) {
            Log.d(TAG, "notifyProgress: Activity or progress is null");
            return;
        }

        activity.runOnUiThread(() -> progressBar.setProgress(progress));
    }
}
