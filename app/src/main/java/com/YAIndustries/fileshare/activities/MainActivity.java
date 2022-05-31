package com.YAIndustries.fileshare.activities;

import static com.YAIndustries.fileshare.utilities.Utils.checkLocationPermission;
import static com.YAIndustries.fileshare.utilities.Utils.checkReadStoragePermission;
import static com.YAIndustries.fileshare.utilities.Utils.checkWriteStoragePermission;
import static com.YAIndustries.fileshare.utilities.Utils.showToast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
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

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private Button sendButton, receiveButton, testButton, searchButton;
    private WifiP2pManager manager;
    private Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private AppCompatActivity thisActivity;
    private TextView deviceListView, filePathView;
    private PeerListListener peerListListener;
    private List<WifiP2pDevice> availableDeviceList = new ArrayList<>();
    private Uri filePath;
    private TextInputLayout portAddress, ipAddress;
    private String hostAddress = null;

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

        peerListListener = deviceList -> {
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

        ActivityResultLauncher<String> filePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri result) -> {
                    filePath = result;
                    filePathView.setText(filePath.toString());
                }
        );

        ConnectionInfoListener connectionInfoListener = (WifiP2pInfo info) -> {
            hostAddress = info.groupOwnerAddress.getHostAddress();
            showToast(this, "Host Address is : "+ hostAddress);
            ipAddress.getEditText().setText(hostAddress);
        };

        searchButton.setOnClickListener((listener) -> {
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
        });

        sendButton.setOnClickListener((listener) -> {
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
        });

        receiveButton.setOnClickListener((listener) -> {
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
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.execute(() -> {
                String res = createServer();
                Log.d("Server Result", res+":Error");
            });
        });

        deviceListView.setOnClickListener((listener) -> {
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
        });

        testButton.setOnClickListener((listener) -> {
            if(!checkWriteStoragePermission(this) || !checkReadStoragePermission(this)){
                showToast(this, "Storage Permission not Granted", Toast.LENGTH_SHORT);
                return;
            }
            if(filePath == null)
                filePicker.launch("*/*");
            try {
                File file = new File(Environment.getExternalStorageDirectory() + "/"
                        +"FileShare/" + System.currentTimeMillis()
                        + ".jpg");
                ContentResolver cr = getContentResolver();
                InputStream fis = cr.openInputStream(filePath);
                File directory = file.getParentFile();
                if (directory == null){
                    Log.d("Directory Check", "Directory not created");
                    return;
                }

                if(!directory.isDirectory()){
                    directory.mkdir();
                }


                boolean fileCreated = file.createNewFile();
                if(fileCreated)
                    showToast(this, "File creation Successful");
                FileOutputStream fos = new FileOutputStream(file);
                Log.d("Directory Check", "File Created : "+ fileCreated);
                Executors.newSingleThreadExecutor().execute(() -> Utils.copyFile(this, fis, fos));

            }catch (Exception e) {
                e.printStackTrace();
                Log.d("File Handler", e.getMessage());
            }
        });



        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new MyBroadcastReceiver(manager, channel, this, peerListListener, connectionInfoListener);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
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

    private String createServer() {
        try {
            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(0);
            Log.d("Server Creation ", "Port "+ serverSocket.getLocalPort());
            Log.d("Server Creation", "Address" + serverSocket.getInetAddress().toString());
            Socket client = serverSocket.accept();
            Log.d("Server Creation","Address "+ client.getInetAddress().toString());
            runOnUiThread(()-> {
                showToast(this, "Server Created at Port " + serverSocket.getLocalPort());
                portAddress.getEditText().setText(client.getInetAddress().toString());
            });
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            File file = new File(Environment.getExternalStorageDirectory() + "/"
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
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.d("Service Executor", e.getMessage());
            return null;
        } catch (Exception e) {
            Log.d("Service Executor Exception ", e.getMessage());
            runOnUiThread(() -> showToast(this, e.getMessage()));
            return null;
        }
    }

    private String sendDataToServer(String hostAddress, int port) throws Exception {
        Socket socket = new Socket();
        String ret = null;
        try {
            socket.bind(null);
            socket.connect(new InetSocketAddress(hostAddress, port), 500);

            runOnUiThread(() -> showToast(this, "Connection Successful"));

            /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data is retrieved by the server device.
             */
            OutputStream outputStream = socket.getOutputStream();
            ContentResolver cr = getContentResolver();
            InputStream inputStream = cr.openInputStream(filePath);
            Utils.copyFile(this, inputStream, outputStream);
            inputStream.close();
            outputStream.close();
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
}