package com.YAIndustries.fileshare.activities;

import static com.YAIndustries.fileshare.utilities.Utils.checkLocationPermission;
import static com.YAIndustries.fileshare.utilities.Utils.checkReadStoragePermission;
import static com.YAIndustries.fileshare.utilities.Utils.checkWriteStoragePermission;
import static com.YAIndustries.fileshare.utilities.Utils.showToast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.YAIndustries.fileshare.R;
import com.YAIndustries.fileshare.services.MyBroadcastReceiver;
import com.YAIndustries.fileshare.utilities.Utils;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private Button sendButton, receiveButton, testButton, searchButton;
    private WifiP2pManager manager;
    private Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private AppCompatActivity thisActivity;
    private TextView deviceListView, filePathView;
    private final List<WifiP2pDevice> availableDeviceList = new ArrayList<>();
    private Uri filePath;
    private TextInputLayout portAddress, ipAddress;
    private String hostAddress = null;
    private final ActivityResultLauncher<String> filePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            (Uri result) -> {
                filePath = result;
                filePathView.setText(filePath.toString());
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisActivity = this;
        sendButton = findViewById(R.id.sendButton);
        receiveButton = findViewById(R.id.receiveButton);
        deviceListView = findViewById(R.id.deviceListView);
        filePathView = findViewById(R.id.filePathView);
        portAddress = findViewById(R.id.portFieldLayout);
        testButton = findViewById(R.id.testStorage);
        searchButton = findViewById(R.id.search_button);
        ipAddress = findViewById(R.id.ipAddressInput);

        PeerListListener peerListListener = deviceList -> {
            Collection<WifiP2pDevice> currentPeers = deviceList.getDeviceList();
            if(!availableDeviceList.equals(currentPeers)) {
                availableDeviceList.clear();
                availableDeviceList.addAll(currentPeers);
            }

            if(availableDeviceList.isEmpty())
                deviceListView.setText("No Device Found");
            else
                deviceListView.setText(availableDeviceList.toString());
        };
        filePath = null;

        ConnectionInfoListener connectionInfoListener = (WifiP2pInfo info) -> {
            hostAddress = info.groupOwnerAddress.getHostAddress();
            showToast(this, "Host Address is : "+ hostAddress);
            ipAddress.getEditText().setText(hostAddress);
        };

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new MyBroadcastReceiver(manager, channel, this, peerListListener, connectionInfoListener);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        searchButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        receiveButton.setOnClickListener(this);
        deviceListView.setOnClickListener(this);
        testButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void createServer() {
        try {
            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(0);
            Log.d("Server Creation ", "Port "+ serverSocket.getLocalPort());
            runOnUiThread(()-> {
                portAddress.getEditText().setText(Integer.toString(serverSocket.getLocalPort()));
            });
            Log.d("Server Creation", "Address" + serverSocket.getInetAddress().toString());
            Socket client = serverSocket.accept();
            Log.d("Server Creation","Address "+ client.getInetAddress().toString());
            runOnUiThread(()-> {
                ipAddress.getEditText().setText(client.getInetAddress().toString());
            });
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            File file = new File(Environment.getExternalStorageDirectory() + "/Download/"
                    +"FileShare/" + System.currentTimeMillis()
                    + ".jpg");
            File directory = file.getParentFile();
            if (directory == null || !directory.mkdirs()){
                Log.d("Directory Check", "Directory not created");
                runOnUiThread(() -> showToast(this, "File Cannot be Created"));
            }

            boolean fileCreated = file.createNewFile();
            if(fileCreated)
                runOnUiThread(() -> showToast(this, "File creation Successful"));
            Log.d("Directory Check", "File Created : "+ fileCreated);
            InputStream inputstream = client.getInputStream();
            Utils.copyFile(this, inputstream, new FileOutputStream(file));
            client.close();
            serverSocket.close();
        } catch (Exception e) {
            Log.d("Service Executor Exception ", e.getMessage());
            runOnUiThread(() -> showToast(this, e.getMessage()));
        }
    }

    private String sendDataToServer(String hostAddress, int port) {
        Socket socket = new Socket();
        String ret = null;
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(hostAddress, port), 500);
            runOnUiThread(() -> showToast(this, "Connection Successful"));
            OutputStream outputStream = socket.getOutputStream();
            var inputStream = getContentResolver().openInputStream(filePath);
            Utils.copyFile(this, inputStream, outputStream);
            runOnUiThread(()-> showToast(this,"Data Sent Successfully"));
        } catch (Exception e) {
            Log.d("Send Data to Server", e.getMessage());
            ret = e.getMessage();
        }
        //Cleanup
        finally {
            if (socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.d("Send Data to Server", e.getMessage());
                    ret = e.getMessage();
                }
            }
        }
        if(ret == null)
            return "File Transfer Successful";
        return  ret;
    }

    private void cleanUp() {
        manager.removeGroup(channel, new ActionListener() {
            @Override
            public void onSuccess() {
                showToast(thisActivity, "Cleanup Successful");
                Log.d("Cleanup", "Successful");
            }

            @Override
            public void onFailure(int i) {
                showToast(thisActivity, "Cleanup Error");
                Log.d("Cleanup", "Unsuccessful");
            }
        });
    }

    @Override
    public void onClick(View view) {
        if(view == sendButton) {
            if(filePath == null) {
                filePicker.launch("*/*");
                return;
            }
            String hostAddress = ipAddress.getEditText().getText().toString();
            int port = Integer.parseInt(portAddress.getEditText().getText().toString());
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    sendDataToServer(hostAddress, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        if(view == searchButton) {
            if(!checkLocationPermission(this))
                return;
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("Wifip2p Action Listener", "Success");
                    showToast(thisActivity, "Discover Peers Success", Toast.LENGTH_SHORT);
                }

                @Override
                public void onFailure(int reasonCode) {
                    Log.d("Wifip2p Action Listener", "Failure Reason Code : "+ reasonCode);
                }
            });
        }

        if(view == receiveButton) {
            manager.createGroup(channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(()-> showToast(thisActivity, "P2P group creation Success", Toast.LENGTH_SHORT));
                }

                @Override
                public void onFailure(int i) {
                    runOnUiThread(()-> showToast(thisActivity, "P2P group creation failed. Retry.", Toast.LENGTH_SHORT));
                }
            });
            showToast(this, "Receive Button Clicked");
            if(!checkReadStoragePermission(this) || !checkWriteStoragePermission(this))
                return;
            CompletableFuture.runAsync(this::createServer).thenRunAsync(this::cleanUp);
        }

        if(view == deviceListView) {
            if(availableDeviceList.isEmpty())
                return;

            var connectionDevice = availableDeviceList.get(0);
            var config = new WifiP2pConfig();
            config.deviceAddress = connectionDevice.deviceAddress;
            config.groupOwnerIntent = 0;
            Utils.checkLocationPermission(thisActivity);
            manager.connect(channel, config, new ActionListener() {
                public void onSuccess() {
                    Log.d("Connection Stats", config.toString());
                    manager.requestConnectionInfo(channel, (listener) -> {
                        Log.d("Connection", listener.toString());
                    });
                    Log.d("Connection Stats", "Connection Success");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("Connection Stats : ", "Connection Success");
                }
            });
        }

        if(view == testButton) {
            if(!checkWriteStoragePermission(this) || !checkReadStoragePermission(this)){
                showToast(this, "Storage Permission not Granted", Toast.LENGTH_SHORT);
                return;
            }
            if(filePath == null) {
                filePicker.launch("*/*");
                return;
            }

            try {
                var resolver = Environment.getExternalStorageDirectory().toString()+"/Download/Fileshare/"+ System.currentTimeMillis()+".jpg";
                File f = new File(resolver);
                f.getParentFile().mkdir();
                var inputStream = getContentResolver().openInputStream(filePath);
                var fos = new FileOutputStream(f);
                Utils.copyFile(this, inputStream, fos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}