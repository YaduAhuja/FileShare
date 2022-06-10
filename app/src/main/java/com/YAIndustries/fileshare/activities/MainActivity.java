package com.YAIndustries.fileshare.activities;

import static com.YAIndustries.fileshare.utilities.Utils.checkLocationPermission;
import static com.YAIndustries.fileshare.utilities.Utils.checkReadStoragePermission;
import static com.YAIndustries.fileshare.utilities.Utils.checkWriteStoragePermission;
import static com.YAIndustries.fileshare.utilities.Utils.showToast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.YAIndustries.fileshare.R;
import com.YAIndustries.fileshare.models.FileMetaData;
import com.YAIndustries.fileshare.services.MyBroadcastReceiver;
import com.YAIndustries.fileshare.ui.DeviceListViewAdapter;
import com.YAIndustries.fileshare.utilities.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    private Button sendButton, receiveButton, testButton, searchButton;
    private WifiP2pManager manager;
    private Channel channel;
    private ProgressBar fileProgressBar;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private AppCompatActivity thisActivity;
    private TextView filePathView;
    private final List<WifiP2pDevice> availableDeviceList = new ArrayList<>();
    private Uri filePath;
    private TextInputLayout portAddress, ipAddress;
    private String hostAddress = null;
    private FileMetaData metaData;
    private ArrayList<String> tempModel = new ArrayList<>();
    private RecyclerView deviceListView;

    private final ActivityResultLauncher<String> filePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            (Uri result) -> {
                Log.d("Activity Result URI", result + "");
                if (result == null)
                    return;
                filePath = result;
                var queryResult = getContentResolver().query(result, null, null, null, null);
                metaData = Utils.getFileMetaDataFromCursor(queryResult);
                Log.d("MetaData", metaData.name + "  " + metaData.size);
                String json;
                try {
                    json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(metaData);
                } catch (JsonProcessingException e) {
                    Log.d("Json Writer", e.getMessage());
                    json = "File Parsing Error";
                }
                filePathView.setText(json);
            }
    );

    private final ActivityResultLauncher<String[]> permissionsRequestor = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            (Map<String, Boolean> result) -> {

            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisActivity = this;
        sendButton = findViewById(R.id.sendButton);
        receiveButton = findViewById(R.id.receiveButton);
        filePathView = findViewById(R.id.filePathView);
        portAddress = findViewById(R.id.portFieldLayout);
        testButton = findViewById(R.id.testStorage);
        searchButton = findViewById(R.id.search_button);
        ipAddress = findViewById(R.id.ipAddressInput);
        fileProgressBar = findViewById(R.id.fileProgress);
        deviceListView = findViewById(R.id.deviceListView);

        PeerListListener peerListListener = deviceList -> {
            Collection<WifiP2pDevice> currentPeers = deviceList.getDeviceList();
            if (!availableDeviceList.equals(currentPeers)) {
                availableDeviceList.clear();
                availableDeviceList.addAll(currentPeers);
            }
            deviceListView.getAdapter().notifyDataSetChanged();
        };

        filePath = null;

        ConnectionInfoListener connectionInfoListener = (WifiP2pInfo info) -> {
            hostAddress = info.groupOwnerAddress.getHostAddress();
            showToast(this, "Host Address is : " + hostAddress);
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

        var adapter = new DeviceListViewAdapter(this, availableDeviceList);
        deviceListView.setAdapter(adapter);
        deviceListView.setLayoutManager(new LinearLayoutManager(this));
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

//    TODO: Lifecycle Stop Cleanups to be done here
//    @Override
//    protected void onStop() {
//        super.onStop();
//        Log.d("On Stop", "Called");
//        manager.removeGroup(channel, new ActionListener() {
//            @Override
//            public void onSuccess() {
//                Log.d("Cleanup", "Successful");
//            }
//
//            @Override
//            public void onFailure(int i) {
//                Log.d("Cleanup", "Failed");
//            }
//        });
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createServer() {
        try {
            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(0);
            Log.d("Server Creation ", "Port " + serverSocket.getLocalPort());
            Log.d("Server Data", serverSocket.getInetAddress().getHostAddress());
            runOnUiThread(() -> {
                portAddress.getEditText().setText(serverSocket.getLocalPort() + "");
            });
            Log.d("Server Creation", "Address" + serverSocket.getInetAddress().toString());
            Socket client = serverSocket.accept();
            Log.d("Server Creation", "Address " + client.getInetAddress().toString());
            runOnUiThread(() -> {
                ipAddress.getEditText().setText(client.getInetAddress().toString());
            });
            /**
             * If this code is reached, a client has connected and transferred data
             * Save the input stream from the client as a JPEG file
             */
            String basePath = Environment.getExternalStorageDirectory() + "/Download/FileShare/";
            var file = new File(basePath);
            file.mkdirs();
            InputStream inputstream = client.getInputStream();
            metaData = Utils.getFileMetaDataFromStream(inputstream);
            if (metaData == null) throw new Exception("Error in Processing Metadata");
            file = new File(basePath + "/" + metaData.name);
            var fileCreated = file.createNewFile();
            if (fileCreated)
                runOnUiThread(() -> showToast(this, "File creation Successful"));
            Log.d("Directory Check", "File Created : " + fileCreated);
            var outputStream = new FileOutputStream(file);
            Utils.copyFile(this, inputstream, outputStream, fileProgressBar, metaData.size);
            inputstream.close();
            outputStream.close();
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
            String json = new ObjectMapper().writeValueAsString(metaData);
            outputStream.write(json.getBytes());
            outputStream.write(0);
            var inputStream = getContentResolver().openInputStream(filePath);
            Utils.copyFile(this, inputStream, outputStream, fileProgressBar, metaData.size);
            inputStream.close();
            outputStream.close();
            runOnUiThread(() -> showToast(this, "Data Sent Successfully"));
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
        if (ret == null)
            return "File Transfer Successful";
        return ret;
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
        if (view == sendButton) {
            if (filePath == null) {
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

        if (view == searchButton) {
            if (!checkLocationPermission(this))
                return;
            manager.discoverPeers(channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d("Wifip2p Action Listener", "Success");
                    showToast(thisActivity, "Discover Peers Success", Toast.LENGTH_SHORT);
                }

                @Override
                public void onFailure(int reasonCode) {
                    Log.d("Wifip2p Action Listener", "Failure Reason Code : " + reasonCode);
                }
            });
        }

        if (view == receiveButton) {

//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            TODO: Add the Hotspot Based Transfer Facility
//                var mnx = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//                var callBack = new WifiManager.LocalOnlyHotspotCallback() {
//                    @Override
//                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
//                        super.onStarted(reservation);
////                    hotspotReservation = reservation;
//                        Log.d("Dang", reservation.toString());
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                            var config = reservation.getSoftApConfiguration();
//                            Log.d("DANG", "THE PASSWORD IS: "
//                                    + config.getPassphrase()
//                                    + " \n SSID is : "
//                                    + config.getSsid());
//                        } else {
//                            var config = reservation.getWifiConfiguration();
//                            Log.d("DANG", "THE PASSWORD IS: "
//                                    + config.preSharedKey
//                                    + " \n SSID is : "
//                                    + config.SSID);
//                        }
//
//                        reservation.close();
//                    }
//                };
//
//                mnx.startLocalOnlyHotspot(callBack, new Handler());
//            }
//                        Compatible API

            manager.createGroup(channel, new ActionListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> showToast(thisActivity, "P2P group creation Success", Toast.LENGTH_SHORT));
                    Utils.checkLocationPermission(thisActivity);
                    manager.requestGroupInfo(channel, (WifiP2pGroup group) -> {
                        Log.d("Group Info", "Group "+ group);
                        if (group != null) {
                            Log.d("Group Info", "Passphrase " + group.getPassphrase());
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                                Log.d("Group Info", "Frequency" + group.getFrequency());
                        }
                    });
                }

                @Override
                public void onFailure(int i) {
                    runOnUiThread(()-> showToast(thisActivity, "P2P group creation failed. Retry. by "+ i, Toast.LENGTH_SHORT));
                }
            });

//            TODO: Reflection API For 5ghz Group Creation Change for Android 9 and Below
//            try {
//                int channel_setting = 60;
//                Method setWifiP2pChannels = manager.getClass().getMethod("setWifiP2pChannels", WifiP2pManager.Channel.class, int.class, int.class, WifiP2pManager.ActionListener.class);
//                setWifiP2pChannels.invoke(manager, channel, 0, channel_setting, new WifiP2pManager.ActionListener() {
//                    @Override
//                    public void onSuccess() {
//                        Log.d("Group Creation", "Changed channel (" + channel + ") succeeded");
//                    }
//
//                    @Override
//                    public void onFailure(int reason) {
//                        runOnUiThread(() -> Utils.showToast(thisActivity, "Failed to Create 2.4ghz Connection"));
//                    }
//                });
//            } catch (Exception e) {
//                e.printStackTrace();
//            }


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
            checkLocationPermission(thisActivity);
            manager.connect(channel, config, new ActionListener() {
                public void onSuccess() {
//                    Log.d("Connection Stats", config.toString());
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
            var temp = new WifiP2pDevice();
            availableDeviceList.add(temp);
            deviceListView.getAdapter().notifyDataSetChanged();
//            manager.discoverPeers(channel, new ActionListener() {
//                @Override
//                public void onSuccess() {
//
//                }
//
//                @Override
//                public void onFailure(int i) {
//
//                }
//            });
//            manager.removeGroup(channel, new ActionListener() {
//                @Override
//                public void onSuccess() {
//                    Log.d("Group Removal", "Success");
//                }
//
//                @Override
//                public void onFailure(int i) {
//                    Log.d("Group Removal", "Failed");
//                }
//            });
//            var mnx = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//            Log.d("Wifi Manager", mnx.getConnectionInfo().toString());
//            Log.d("Wifi Manager", mnx.getDhcpInfo().toString());
//            String ip = "";
//            try {
//                Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
//                        .getNetworkInterfaces();
//                while (enumNetworkInterfaces.hasMoreElements()) {
//                    NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
//                    if(networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.supportsMulticast())
//                        continue;
//                    var display = networkInterface.getDisplayName();
//                    var list = networkInterface.getInterfaceAddresses();
//                    for(var x : list) {
//                        if(x.getAddress() instanceof Inet6Address)
//                            continue;
//                        Log.d(x.getAddress().toString(), x.getBroadcast().toString());
//                    }
//
//                    Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
//                    Log.d("Interface Address", display + " : " +list.toString());
//                    while (enumInetAddress.hasMoreElements()) {
//                        InetAddress inetAddress = enumInetAddress.nextElement();
//                        if (inetAddress.isSiteLocalAddress()) {
//                            ip += "SiteLocalAddress: "+ inetAddress.getHostAddress();
//
//                        }
//                    }
//                }
//
//            } catch (SocketException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//                ip += "Something Wrong! " + e.toString() + "\n";
//            }
//            Log.d("IP Address", ip);
//            try {
//                var p = Runtime.getRuntime().exec("ping 192.168.43.201");
//                p.waitFor();
//                var os = new BufferedInputStream(p.getInputStream());
//                StringBuilder sb = new StringBuilder();
//                int c;
//                while((c = os.read()) != -1) {
//                    sb.append((char)c);
//                }
//                Log.d("Process Output", p.exitValue()+"");
//                Log.d("Process Output", sb.toString());
//            } catch (IOException | InterruptedException e) {
//                Log.d("Process Output", e.getMessage());
//            }
//            CompletableFuture.runAsync(this::createServer).thenRunAsync(this::cleanUp);
//            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
//            startActivity(intent);
//            CompletableFuture.runAsync(()-> {
//                if(!checkWriteStoragePermission(this) || !checkReadStoragePermission(this)){
//                    showToast(this, "Storage Permission not Granted", Toast.LENGTH_SHORT);
//                    return;
//                }
//                if(filePath == null) {
//                    filePicker.launch("*/*");
//                    return;
//                }
//
//                try {
//                    var f = new File(Environment.getExternalStorageDirectory().toString()+"/Download/Fileshare");
//                    f.mkdir();
//
//                    String tempFilePath = Environment.getExternalStorageDirectory().toString()+"/Download/FileShare/temp.bin";
//                    var fis = getContentResolver().openInputStream(filePath);
//                    f = new File(tempFilePath);
//                    f.createNewFile();
//                    var testFos = new FileOutputStream(f);
//                    testFos.write(new ObjectMapper().writeValueAsBytes(metaData));
//                    testFos.write(0);
//                    Utils.copyFile(this, fis, testFos, fileProgressBar, metaData.size);
//
//                    StringBuilder sb = new StringBuilder();
//                    fis = new FileInputStream(Environment.getExternalStorageDirectory().toString()+"/Download/FileShare/temp.bin");
//                    metaData = Utils.getFileMetaDataFromStream(fis);
//
//                    var resolver = Environment.getExternalStorageDirectory().toString()+"/Download/Fileshare/"+ metaData.name;
//                    f = new File(resolver);
//                    f.getParentFile().mkdir();
//                    var fos = new FileOutputStream(f);
//                    Utils.copyFile(this, fis, fos, fileProgressBar, metaData.size);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
        }
    }
}