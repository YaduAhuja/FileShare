package com.yaindustries.fileshare.utilities;

import static com.yaindustries.fileshare.utilities.Utils.checkVersionAboveP;
import static com.yaindustries.fileshare.utilities.Utils.isLocationPermissionAvailable;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.yaindustries.fileshare.exceptions.PermissionNotFoundException;
import com.yaindustries.fileshare.services.WifiP2pBroadcastReceiver;

public class WifiP2pHelper {
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final AppCompatActivity activity;
    private final PeerListListener peerListListener;
    private final ConnectionInfoListener connectionInfoListener;
    private final WifiP2pBroadcastReceiver broadcastReceiver;
    private final String TAG = "Wifi P2p Helper";

    public WifiP2pHelper(AppCompatActivity activity, PeerListListener peerListListener, ConnectionInfoListener connectionInfoListener) {
        this.activity = activity;
        this.peerListListener = peerListListener;
        this.connectionInfoListener = connectionInfoListener;
        manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(activity, activity.getMainLooper(), null);
        broadcastReceiver = new WifiP2pBroadcastReceiver(activity, manager, channel, peerListListener, connectionInfoListener);
    }


    @SuppressLint("MissingPermission")
    public void discoverPeers(ActionListener actionListener) throws PermissionNotFoundException {
        if (!isLocationPermissionAvailable(activity))
            throw new PermissionNotFoundException();
        manager.discoverPeers(channel, actionListener);
    }

    @SuppressLint("MissingPermission")
    public void connectToDevice(WifiP2pDevice device, ActionListener actionListener) throws PermissionNotFoundException {
        if (!isLocationPermissionAvailable(activity))
            throw new PermissionNotFoundException();

        var config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        manager.connect(channel, config, actionListener);
    }

    @SuppressLint({"MissingPermission", "NewApi"})
    public void createGroup(ActionListener actionListener) throws PermissionNotFoundException {
        if (!isLocationPermissionAvailable(activity))
            throw new PermissionNotFoundException();

        if (checkVersionAboveP()) {
            var builder = new WifiP2pConfig.Builder();
            builder.setNetworkName("DIRECT-" + Utils.getDeviceName());
            builder.setPassphrase("123456789");
            builder.setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_5GHZ);
            manager.createGroup(channel, builder.build(), actionListener);
            Log.d(TAG, "createGroup: using 5ghz band" );
        } else
            manager.createGroup(channel, actionListener);
    }

    public void getConnectionInfo() throws PermissionNotFoundException {
        manager.requestConnectionInfo(channel, connectionInfoListener);
    }

    public void removeGroup(ActionListener actionListener) {
        manager.removeGroup(channel, actionListener);
    }

    public BroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }
}
