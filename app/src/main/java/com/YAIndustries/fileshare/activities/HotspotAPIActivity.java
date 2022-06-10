package com.YAIndustries.fileshare.activities;

import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.YAIndustries.fileshare.R;
import com.YAIndustries.fileshare.models.FileMetaData;
import com.YAIndustries.fileshare.utilities.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class HotspotAPIActivity extends AppCompatActivity implements OnClickListener {
    private boolean receiving;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private final String TAG = "HotspotAPIActivity";
    private ProgressBar fileTransferProgressBar;
    private TextInputLayout fileTransferPortInput;
    private TextView fileDetailsView;
    private Uri fileUri;
    private FileMetaData metaData;
//    private Queue<Runnable> executionsPipeline;

    private final ActivityResultLauncher<String> filePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            (Uri result) -> {
                Log.d("Activity Result URI", result + "");
                if (result == null)
                    return;
                fileUri = result;
                metaData = Utils.getFileMetaDataFromUri(this, result);
                if (metaData != null)
                    fileDetailsView.setText(metaData.name);
                CompletableFuture.runAsync(this::connectToServer);
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotspot);

        // UI Objects
        Button sendButton = findViewById(R.id.sendButtonHotspot);
        Button receiveButton = findViewById(R.id.receiveButtonHotspot);
        fileTransferPortInput = findViewById(R.id.fileTransferPortAddressInput);
        fileTransferProgressBar = findViewById(R.id.fileProgressBarHotspot);
        fileDetailsView = findViewById(R.id.fileDetailsView);
//        executionsPipeline = new LinkedList<>();



        //OnClickListeners Setting
        sendButton.setOnClickListener(this);
        receiveButton.setOnClickListener(this);


        //Permissions Request
        if(Utils.checkStoragePermissions(this))
            Utils.requestStoragePermissions(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if(clientSocket != null)
                clientSocket.close();
            if(clientSocket != null && receiving)
                serverSocket.close();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage() + " While Closing connections");
        }
    }


    @Override
    public void onClick(View v) {
        if(!Utils.checkStoragePermissions(this)) {
            Utils.showToast(this, "Storage Permissions Not Granted Unable to Read/ Write Files");
            return;
        }

        if(v.getId() == R.id.sendButtonHotspot) {
            filePicker.launch("*/*");

        }

        if(v.getId() == R.id.receiveButtonHotspot) {
            receiving = true;
            CompletableFuture.runAsync(this::createServer);
        }
    }


    private void createServer() {
        try {
            serverSocket = new ServerSocket(0);
            int portAddress = serverSocket.getLocalPort();

            runOnUiThread(() -> fileTransferPortInput.getEditText().setText(Integer.toString(portAddress)));

            Log.d(TAG, "Server Port Address : " + portAddress);
            Log.d(TAG, "Server Inet Address : " + serverSocket.getInetAddress());
            Log.d(TAG, "Server Local Socket Address : " + serverSocket.getLocalSocketAddress());

            clientSocket = serverSocket.accept();

            Log.d(TAG, "Client Port Address : " + clientSocket.getPort());
            Log.d(TAG, "Client Inet Address : " + clientSocket.getInetAddress());
            Log.d(TAG, "Client Remote Socket Address : " + clientSocket.getRemoteSocketAddress());

            var inputStream = clientSocket.getInputStream();
            var metaData = Utils.getFileMetaDataFromStream(inputStream);
            if(metaData == null) {

            }
            File f = new File(Utils.getDataDirectoryPath() + metaData.name);
            var outputStream = new FileOutputStream(f);
            Utils.copyFile(HotspotAPIActivity.this, inputStream, outputStream, fileTransferProgressBar, metaData.size);
            cleanup();
            runOnUiThread(() -> Utils.showToast(HotspotAPIActivity.this, "File Received Successfully"));

        } catch (Exception e) {
            Log.d(TAG, "Exception in Create Server "+ e.getMessage());
        }
    }

    private void connectToServer() {
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        var info = manager.getConnectionInfo();
        var dhcpInfo = manager.getDhcpInfo();

        runOnUiThread(() -> Utils.showToast(HotspotAPIActivity.this,
                "Using Connection " + info.getSSID() + " and Frequency "+ info.getFrequency()));
        Log.d(TAG, "connectToServer: " + info.toString());
        Log.d(TAG, "connectToServer: " + dhcpInfo.toString());

        try {
            clientSocket = new Socket();
            clientSocket.bind(null);
            InetAddress address = InetAddress.getByAddress(Utils.getByteArrayFromInt(dhcpInfo.serverAddress));
            Log.d(TAG, "connectToServer: " + address);
            clientSocket.connect(new InetSocketAddress(address.getHostName(), Integer.parseInt(fileTransferPortInput.getEditText().getText().toString())), 500);
            runOnUiThread(() -> Utils.showToast(this, "Connection Successful", Toast.LENGTH_SHORT));
            var json = new ObjectMapper().writeValueAsString(metaData);
            var outputStream = clientSocket.getOutputStream();
            outputStream.write(json.getBytes());
            outputStream.write(0);
            var inputStream = getContentResolver().openInputStream(fileUri);
            var res = Utils.copyFile(HotspotAPIActivity.this, inputStream, outputStream, fileTransferProgressBar, metaData.size);
            cleanup();
            var resMessage = res ? "File Sent Successfully": "Error in Sending File";
            runOnUiThread(() -> Utils.showToast(HotspotAPIActivity.this, resMessage));
        } catch (IOException e) {
            Log.d(TAG, "connectToServer: " + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            if (clientSocket != null && clientSocket.isConnected())
                clientSocket.close();
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
            Log.d(TAG, "Cleanup: Successful");
        } catch (IOException e) {
            Log.d(TAG, "Cleanup: " + e.getMessage());
        }
    }
}
