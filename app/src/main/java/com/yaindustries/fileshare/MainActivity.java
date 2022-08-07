package com.yaindustries.fileshare;

import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.yaindustries.fileshare.database.DBRepository;
import com.yaindustries.fileshare.database.FileShareDatabase;
import com.yaindustries.fileshare.models.FileMetaData;
import com.yaindustries.fileshare.models.Pair;
import com.yaindustries.fileshare.utilities.ConnectionHelper;
import com.yaindustries.fileshare.utilities.WifiP2pHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main Activity";

    public ConnectionHelper connectionHelper;
    public WifiP2pHelper wifiP2pHelper;
    public List<Pair<FileMetaData, Uri>> sendFilesQueue = new ArrayList<>();
    public boolean sending;
    public DBRepository dbRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Change This in Later Update
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbRepository = new DBRepository(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Main Activity Destroyed");
        if (connectionHelper != null)
            try {
                connectionHelper.cleanup();
            } catch (IOException e) {
                Log.d(TAG, "onDestroy: " + e);
            }

        if (wifiP2pHelper != null)
            wifiP2pHelper.removeGroup(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Group Removal Success");
                }

                @Override
                public void onFailure(int i) {
                    Log.d(TAG, "Group Removal Failed");
                }
            });
        super.onDestroy();
    }
}