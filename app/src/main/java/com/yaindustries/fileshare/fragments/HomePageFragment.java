package com.yaindustries.fileshare.fragments;

import android.Manifest;
import android.os.Bundle;
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


public class HomePageFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "HomePage Fragment";
    private NavController navController;
    private Button sendButton, receiveButton;
    private ActivityResultLauncher<String[]> locationRequest;
    private boolean isLocationPermissionAvailable = false;

    public HomePageFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var view = inflater.inflate(R.layout.fragment_home_page, container, false);
        initializeViews(view);

        locationRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), (result) -> {
            boolean isAvailable = true;
            for (String key : result.keySet())
                isAvailable = isAvailable && result.get(key);
            if (isAvailable)
                Log.d(TAG, "onCreateView: " + "Permission Granted");
            else
                Log.d(TAG, "onCreateView: " + "Permission Denied");
            isLocationPermissionAvailable = isAvailable;
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        locationRequest.launch(permissions);
    }

    @Override
    public void onClick(View view) {
        if (!Utils.isLocationPermissionAvailable(getActivity()))
            return;
        if (view == sendButton)
            navController.navigate(R.id.sendFragment);
        if (view == receiveButton)
            navController.navigate(R.id.receiveFragment);
    }


    public void initializeViews(View view) {
        sendButton = view.findViewById(R.id.sendButton);
        receiveButton = view.findViewById(R.id.receiveButton);

        sendButton.setOnClickListener(this);
        receiveButton.setOnClickListener(this);
    }
}