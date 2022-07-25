package com.yaindustries.fileshare.fragments;

import static android.content.Context.LOCATION_SERVICE;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.utilities.Utils;

import java.util.ArrayList;


public class HomePageFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "HomePage Fragment";
    private NavController navController;
    private Button sendButton, receiveButton;
    private ActivityResultLauncher<String[]> locationRequest;
    private LocationManager locationManager;
    private Runnable pendingTask;

    public HomePageFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var view = inflater.inflate(R.layout.fragment_home_page, container, false);
        initialize(view);

        locationRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (result) -> {
            Log.d(TAG, "onCreateView: " + result);
            boolean isAvailable = true;
            for (String key : result.keySet())
                isAvailable = isAvailable && result.get(key);
            if (isAvailable)
                Log.d(TAG, "onCreateView: Permission Granted");
            else
                Log.d(TAG, "onCreateView: Permission Denied");
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        ArrayList<String> permissions = new ArrayList<>();
        if(!Utils.isLocationPermissionAvailable(getContext()))
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if(!Utils.isStoragePermissionAvailable(getContext())) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if(!permissions.isEmpty())
            locationRequest.launch(permissions.toArray(new String[0]));
    }

    @Override
    public void onClick(View view) {
        if (!Utils.isLocationPermissionAvailable(getContext())) {
            Utils.showToast(getContext(), "File Share cannot work without Location Permission");
            return;
        }

        if(Utils.isStoragePermissionAvailable(getContext())) {
            Utils.showToast(getContext(), "File Share cannot work without Storage Permission");
            return;
        }

        if(!isLocationOn()) {
            var dialog = new AlertDialog.Builder(getContext())
                    .setTitle("Location Services Required")
                    .setMessage("After android 6.0 Location Services are required to use the Wifi modules." +
                            "Please turn on location services.")
                    .setPositiveButton(R.string.Ok, (dialogInterface, id) -> {
                        var intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.Cancel, (dialogInterface, i) ->
                        Utils.showToast(getContext(), "Location Services are Needed to run File Share")
                    )
                    .create();
            dialog.show();
        }
        pendingTask = () -> navigateToFragments(view);
    }


    public void initialize(View view) {
        sendButton = view.findViewById(R.id.sendButton);
        receiveButton = view.findViewById(R.id.receiveButton);

        sendButton.setOnClickListener(this);
        receiveButton.setOnClickListener(this);
        locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
    }


    @Override
    public void onStart() {
        super.onStart();
        isLocationOn();
    }

    @Override
    public void onResume() {
        super.onResume();
        isLocationOn();
        if(pendingTask != null)
            pendingTask.run();
    }

    private boolean isLocationOn() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void navigateToFragments(View view) {
        if (view == sendButton)
            navController.navigate(R.id.fileFragment);
        if (view == receiveButton)
            navController.navigate(R.id.receiveFragment);
    }
}