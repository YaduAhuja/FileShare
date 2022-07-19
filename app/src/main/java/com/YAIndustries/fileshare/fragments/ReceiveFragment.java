package com.yaindustries.fileshare.fragments;

import android.annotation.SuppressLint;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

public class ReceiveFragment extends Fragment {

    private final String TAG = "Receive Fragment";
    private ConnectionHelper connectionHelper;
    private WifiP2pHelper wifiP2pHelper;
    private NavController navController;
    private MainActivity activity;

    public ReceiveFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_receive, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        assert activity != null;
        navController = Navigation.findNavController(view);
        wifiP2pHelper = new WifiP2pHelper((AppCompatActivity) getActivity(), null, null);
        connectionHelper = new ConnectionHelper();
        activity.connectionHelper = connectionHelper;
        activity.wifiP2pHelper = wifiP2pHelper;
        CompletableFuture.runAsync(this::startRegistration).thenRunAsync(this::startListening);
    }

    private void startListening() {
        boolean connected = false;
        try {
            connectionHelper.listenForConnection();
            connected = true;
        } catch (IOException ignored) {}

        if (connected) {
            activity.sending = false;
            activity.runOnUiThread(() -> navController.navigate(R.id.transferFragment));
        }
    }


    @SuppressLint("MissingPermission")
    private void startRegistration() {
        try {
            var port = connectionHelper.createServer();
            Log.d(TAG, "Server Started at port :" + port);
            activity.runOnUiThread(() -> Utils.showToast(activity, "Server Started at Port "+ port));
            wifiP2pHelper.createGroup(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Wifi P2p Group Creation Success");
                }

                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "Wifi P2p Group Creation Failed " + i);
                 }
            });
        } catch (NoPortAvailableException | PermissionNotFoundException e) {
            Log.d(TAG, "" + e);
        }
    }
}