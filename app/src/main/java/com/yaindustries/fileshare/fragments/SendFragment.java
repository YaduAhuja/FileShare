package com.yaindustries.fileshare.fragments;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.yaindustries.fileshare.MainActivity;
import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.exceptions.NoPortAvailableException;
import com.yaindustries.fileshare.exceptions.PermissionNotFoundException;
import com.yaindustries.fileshare.ui.SendFragmentRecyclerViewAdapter;
import com.yaindustries.fileshare.utilities.ConnectionHelper;
import com.yaindustries.fileshare.utilities.Utils;
import com.yaindustries.fileshare.utilities.WifiP2pHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SendFragment extends Fragment {
    private final String TAG = "Send Fragment";
    private Button searchDevicesButton;
    private WifiP2pHelper wifiP2pHelper;
    private BroadcastReceiver broadcastReceiver;
    private ConnectionHelper connectionHelper;
    private IntentFilter intentFilter;
    private NavController navController;
    private MainActivity activity;
    private final List<WifiP2pDevice> deviceList;
    private RecyclerView deviceRecyclerView;
    private SendFragmentRecyclerViewAdapter adapter;
    private boolean connecting = false;

    public SendFragment() {
        deviceList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var view = inflater.inflate(R.layout.fragment_send, container, false);
        initializeViews(view);

        PeerListListener peerListListener = wifiP2pDeviceList -> {
            deviceList.clear();
            deviceList.addAll(wifiP2pDeviceList.getDeviceList());
            Log.d(TAG, "onCreateView: " + deviceList);
            adapter.notifyDataSetChanged();
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
        try {
            connectionHelper.createClientSocket();
        } catch (IOException e) {
            Log.d(TAG, "onViewCreated: Cannot Create Socket");
        }
        searchDevices();
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
        searchDevicesButton = view.findViewById(R.id.searchDevices);
        deviceRecyclerView = view.findViewById(R.id.deviceRecyclerView);
        adapter = new SendFragmentRecyclerViewAdapter(deviceList, position -> {
            if (connecting)
                return;
            connecting = true;
            var device = deviceList.get(position);
            try {

                wifiP2pHelper.connectToDevice(device, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Connected to Device: " + device);
                        connecting = false;
                    }

                    @Override
                    public void onFailure(int i) {
                        Log.d(TAG, "Connection to Device Failed");
                        connecting = false;
                    }
                });
            } catch (PermissionNotFoundException e) {
                Log.d(TAG, "Permission Not Found");
                Utils.showToast(getContext(), "Permission Not Found", Toast.LENGTH_SHORT);
            }
        });
        deviceRecyclerView.setAdapter(adapter);
        searchDevicesButton.setOnClickListener((listener) -> searchDevices());
    }


    private void searchDevices() {
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
            Log.d(TAG, "searchDevices: Unable to Search Peers");
            Log.d(TAG, "searchDevices: " + e);
        }
    }
}