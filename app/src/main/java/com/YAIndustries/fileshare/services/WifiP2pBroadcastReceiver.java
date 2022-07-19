package com.yaindustries.fileshare.services;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;


public class WifiP2pBroadcastReceiver extends BroadcastReceiver {
    private final WifiP2pManager manager;
    private final Channel channel;
    private final AppCompatActivity activity;
    private final PeerListListener peerListListener;
    private final ConnectionInfoListener connectionInfoListener;
    private final String TAG = "Broadcast Receiver";

    public WifiP2pBroadcastReceiver(AppCompatActivity activity, WifiP2pManager manager, Channel channel, PeerListListener peerListListener, ConnectionInfoListener connectionInfoListener) {
        super();
        this.activity = activity;
        this.manager = manager;
        this.channel = channel;
        this.peerListListener = peerListListener;
        this.connectionInfoListener = connectionInfoListener;
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            manager.requestConnectionInfo(channel, connectionInfoListener);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            manager.requestPeers(channel, peerListListener);
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

        }
    }
}
