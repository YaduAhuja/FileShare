package com.YAIndustries.fileshare.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;

import androidx.appcompat.app.AppCompatActivity;

import com.YAIndustries.fileshare.services.MyBroadcastReceiver;

public class WifiP2pHelper {
    private final BroadcastReceiver receiver;
    private final AppCompatActivity activity;
    private final IntentFilter intentFilter;
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;

    public WifiP2pHelper(AppCompatActivity activity, PeerListListener peerListListener, ConnectionInfoListener connectionInfoListener) {
        this.activity = activity;

        manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, activity.getMainLooper(), null);
        receiver = new MyBroadcastReceiver(manager, channel, activity, peerListListener, connectionInfoListener);

        intentFilter = new IntentFilter();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void scanForPeers(Runnable successCode, Runnable failureCode) {
        Utils.checkLocationPermission(activity);
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                successCode.run();
            }

            @Override
            public void onFailure(int i) {
                failureCode.run();
            }
        });
    }

    public void suspend() {
        activity.unregisterReceiver(receiver);
    }

    public void resume() {
        activity.registerReceiver(receiver, intentFilter);
    }
}
