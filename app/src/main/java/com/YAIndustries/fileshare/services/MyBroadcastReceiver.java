package com.YAIndustries.fileshare.services;

import static com.YAIndustries.fileshare.utilities.Utils.checkLocationPermission;
import static com.YAIndustries.fileshare.utilities.Utils.showToast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.YAIndustries.fileshare.utilities.Utils;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private final WifiP2pManager manager;
    private final Channel channel;
    private final AppCompatActivity activity;
    private final PeerListListener peerListListener;
    private final ConnectionInfoListener connectionInfoListener;

    public MyBroadcastReceiver(WifiP2pManager manager, Channel channel, AppCompatActivity activity, PeerListListener peerListListener, ConnectionInfoListener connectionInfoListener) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
        this.peerListListener = peerListListener;
        this.connectionInfoListener = connectionInfoListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("State Action ", action);
        showToast(activity, action, Toast.LENGTH_SHORT);
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            showToast(activity, action);
            Utils.checkLocationPermission(activity);
            manager.requestGroupInfo(channel, (WifiP2pGroup group) -> {
                Log.d("Group Info", "Group " + group);
            });
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            if (!checkLocationPermission(activity))
                return;
            Log.d("Wifip2p Manager Reference", manager.toString());
            if (manager != null)
                manager.requestPeers(channel, peerListListener);

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.d("Network Info", networkInfo.toString());
            if (networkInfo.isConnected())
                manager.requestConnectionInfo(channel, connectionInfoListener);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }
}
