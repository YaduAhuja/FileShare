package com.yaindustries.fileshare.fragments;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.yaindustries.fileshare.MainActivity;
import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.exceptions.NoPortAvailableException;
import com.yaindustries.fileshare.exceptions.PermissionNotFoundException;
import com.yaindustries.fileshare.utilities.ConnectionHelper;
import com.yaindustries.fileshare.utilities.Utils;
import com.yaindustries.fileshare.utilities.WifiP2pHelper;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class SendFragment extends Fragment {
    private final String TAG = "Send Fragment";
    private final ActivityResultLauncher<String> filePicker;
    private TextView textViewDetails;
    private Button selectButton;
    private WifiP2pHelper wifiP2pHelper;
    private BroadcastReceiver broadcastReceiver;
    private ConnectionHelper connectionHelper;
    private IntentFilter intentFilter;
    private NavController navController;
    private MainActivity activity;

    public SendFragment() {
        filePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri result) -> {
                    if (result == null)
                        return;
                    var queryResult = getContext().getContentResolver().query(result, null, null, null, null);
                    var metaData = Utils.getFileMetaDataFromCursor(queryResult);

                    activity.sendFilesQueue.put(metaData, result);
                    Utils.showToast(getContext(), "File Added to Queue", Toast.LENGTH_SHORT);
//                    navController.navigate(R.id.transferFragment);
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var view = inflater.inflate(R.layout.fragment_send, container, false);
        initializeViews(view);

        PeerListListener peerListListener = wifiP2pDeviceList -> {
            var deviceList = wifiP2pDeviceList.getDeviceList().toArray(new WifiP2pDevice[0]);
            textViewDetails.setText(wifiP2pDeviceList.toString());
            textViewDetails.setOnClickListener((listener) -> {
                textViewDetails.setOnClickListener(null);
                try {
                    wifiP2pHelper.connectToDevice(deviceList[0], new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Connected to Device: " + deviceList[0]);
                        }

                        @Override
                        public void onFailure(int i) {
                            Log.d(TAG, "Connection to Device Failed");
                        }
                    });
                } catch (PermissionNotFoundException e) {
                    Log.d(TAG, "Permission Not Found");
                    Utils.showToast(getContext(), "Permission Not Found", Toast.LENGTH_SHORT);
                }
            });
        };

        ConnectionInfoListener connectionInfoListener = wifiP2pInfo -> {
            Log.d(TAG, "Connection Info Listener: " + wifiP2pInfo);
            if (!wifiP2pInfo.groupFormed)
                return;
            CompletableFuture.runAsync(() -> {
                try {
                    connectionHelper.connectToServerSocket(wifiP2pInfo.groupOwnerAddress);
                    activity.sending = true;
                    navController.navigate(R.id.transferFragment);
                } catch (NoPortAvailableException e) {
                    Log.d(TAG, "Connection Info Listener : Connection Failed" + e);
                    Utils.showToast(getContext(), "Connection Failed ", Toast.LENGTH_SHORT);
                }
            });
        };
        activity = (MainActivity) getActivity();
        wifiP2pHelper = new WifiP2pHelper(activity, peerListListener, connectionInfoListener);
        broadcastReceiver = wifiP2pHelper.getBroadcastReceiver();
        connectionHelper = new ConnectionHelper();
        activity.wifiP2pHelper = wifiP2pHelper;
        activity.connectionHelper = connectionHelper;
        initializeIntentFilter();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        startNetworkStack();
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.registerReceiver(broadcastReceiver, intentFilter);
        Log.d(TAG, "onStart: ");
    }

    @Override
    public void onStop() {
        activity.unregisterReceiver(broadcastReceiver);
        super.onStop();
    }

    private void initializeIntentFilter() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void initializeViews(@NonNull View view) {
        textViewDetails = view.findViewById(R.id.textViewSendDetails);
        selectButton = view.findViewById(R.id.selectFileButton);
        selectButton.setOnClickListener((listener) -> {
            filePicker.launch("*/*");
        });
    }


    private void startNetworkStack() {
        try {
            wifiP2pHelper.discoverPeers(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Discover Peers Request Success");
                }

                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "Discover Peers Request Failed");
                }
            });
        } catch (PermissionNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connectionHelper.createClientSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}