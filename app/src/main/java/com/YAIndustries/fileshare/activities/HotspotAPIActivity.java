package com.YAIndustries.fileshare.activities;

import android.net.Uri;
import android.net.nsd.NsdManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.YAIndustries.fileshare.R;
import com.YAIndustries.fileshare.interfaces.NsdTaskInvoker;
import com.YAIndustries.fileshare.models.FileMetaData;
import com.YAIndustries.fileshare.utilities.ConnectionHelper;
import com.YAIndustries.fileshare.utilities.NsdHelper;
import com.YAIndustries.fileshare.utilities.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

public class HotspotAPIActivity extends AppCompatActivity implements OnClickListener, NsdTaskInvoker {
    private final String TAG = "HotspotAPIActivity";
    private ProgressBar fileTransferProgressBar;
    private TextView fileDetailsView;
    private Uri fileUri;
    private FileMetaData metaData;
    private NsdHelper nsdHelper;
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
                nsdHelper.discoverServices();
            }
    );
    private ConnectionHelper connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotspot);

        // UI Objects
        Button sendButton = findViewById(R.id.sendButtonHotspot);
        Button receiveButton = findViewById(R.id.receiveButtonHotspot);
        fileTransferProgressBar = findViewById(R.id.fileProgressBarHotspot);
        fileDetailsView = findViewById(R.id.fileDetailsView);
//        executionsPipeline = new LinkedList<>();


        // Helpers
        connection = new ConnectionHelper();
        nsdHelper = new NsdHelper(this, (NsdManager) getSystemService(NSD_SERVICE));


        //OnClickListeners Setting
        sendButton.setOnClickListener(this);
        receiveButton.setOnClickListener(this);


        //Permissions Request
        if (Utils.checkStoragePermissions(this))
            Utils.requestStoragePermissions(this);
    }

    @Override
    public void onClick(View v) {
        if (!Utils.checkStoragePermissions(this)) {
            Utils.showToast(this, "Storage Permissions Not Granted Unable to Read/ Write Files");
            return;
        }

        if (v.getId() == R.id.sendButtonHotspot) {
            filePicker.launch("*/*");
            //Check FilePicker Result for Connections
        }

        if (v.getId() == R.id.receiveButtonHotspot) {
            CompletableFuture.runAsync(this::createServer);
        }
    }


    private void createServer() {
        try {
            int portAddress = connection.createServer();
            nsdHelper.registerService(portAddress);
            connection.listenForConnection();
            connection.readDataFromSocket(this, fileTransferProgressBar);

            runOnUiThread(() -> Utils.showToast(HotspotAPIActivity.this, "File Received Successfully"));
        } catch (Exception e) {
            Log.d(TAG, "Exception in Create Server " + e.getMessage());
        }
    }

    public void connectToServer(InetAddress address, int port) {
        try {
            connection.createClientSocket();
            connection.connectToServerSocket(address, port);
            connection.pushDataToSocket(this, fileTransferProgressBar, fileUri);

            runOnUiThread(() -> Utils.showToast(HotspotAPIActivity.this, "File Transfer Successful"));
        } catch (IOException e) {
            Log.d(TAG, "connectToServer: " + e.getMessage());
            runOnUiThread(() -> Utils.showToast(HotspotAPIActivity.this, "File Transfer Failed"));
        }
    }

    @Override
    public void onNsdTaskSuccessful(InetAddress address, int port) {
        connectToServer(address, port);
    }

    @Override
    public void onNsdTaskFailed() {

    }

    @Override
    protected void onDestroy() {
        try {
            connection.cleanup();
        } catch (IOException e) {
            Log.d(TAG, "Error While Cleaning Up Sockets");
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        nsdHelper.suspend();
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        nsdHelper.resume();
    }
}
