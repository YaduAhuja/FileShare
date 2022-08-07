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
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaindustries.fileshare.MainActivity;
import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.database.DBRepository;
import com.yaindustries.fileshare.database.FileShareDatabase;
import com.yaindustries.fileshare.interfaces.OnResultTask;
import com.yaindustries.fileshare.models.FileMetaData;
import com.yaindustries.fileshare.ui.HomeFragmentRecyclerViewAdapter;
import com.yaindustries.fileshare.utilities.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class HomePageFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "HomePage Fragment";
    private NavController navController;
    private Button sendButton, receiveButton;
    private ActivityResultLauncher<String[]> locationRequest;
    private LocationManager locationManager;
    private Runnable pendingTask;
    private MainActivity mainActivity;
    private RecyclerView fileRecyclerView;
    private final List<FileMetaData> fileInfoList = new ArrayList<>();
    private HomeFragmentRecyclerViewAdapter adapter;
    private FragmentActivity curActivity;
    private TextView recyclerViewAlternateTv;

    public HomePageFragment() {}

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

        if(!Utils.isStoragePermissionAvailable(getContext())) {
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
            pendingTask = () -> navigateToFragments(view);
        } else
            navigateToFragments(view);
    }


    public void initialize(View view) {
        //Initializing Members
        mainActivity = (MainActivity) getActivity();
        curActivity = getActivity();
        locationManager = (LocationManager) getContext().getSystemService(LOCATION_SERVICE);
        adapter = new HomeFragmentRecyclerViewAdapter(fileInfoList);

        //Initializing Views
        sendButton = view.findViewById(R.id.sendButton);
        receiveButton = view.findViewById(R.id.receiveButton);
        fileRecyclerView = view.findViewById(R.id.homePageRecyclerView);
        recyclerViewAlternateTv= view.findViewById(R.id.homePageAlternateRecyclerTv);

        sendButton.setOnClickListener(this);
        receiveButton.setOnClickListener(this);
        fileRecyclerView.setAdapter(adapter);
    }


    @Override
    public void onStart() {
        super.onStart();
        CompletableFuture.runAsync(() -> {
            fileInfoList.clear();
            var data = mainActivity.dbRepository.fileMetaDataDao.getLastNFileMetaData(20);
            fileInfoList.addAll(data);
            if (fileInfoList.isEmpty())
                curActivity.runOnUiThread(() -> {
                    fileRecyclerView.setVisibility(View.INVISIBLE);
                    recyclerViewAlternateTv.setVisibility(View.VISIBLE);
                });
            else
                curActivity.runOnUiThread(() -> {
                    fileRecyclerView.setVisibility(View.VISIBLE);
                    recyclerViewAlternateTv.setVisibility(View.INVISIBLE);
                    adapter.notifyDataSetChanged();
                });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.sendFilesQueue.clear();
        if(isLocationOn() && pendingTask != null) {
            pendingTask.run();
            pendingTask = null;
        }
    }

    private boolean isLocationOn() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void navigateToFragments(View view) {
        if(view == sendButton) {
            CompletableFuture.runAsync(() -> mainActivity.dbRepository.clear());
//            navController.navigate(R.id.sendFragment);
        }

        if (view == receiveButton) {
            CompletableFuture.runAsync(() -> {
                var metaData = new FileMetaData(((int)(Math.random() * 100)) + "dsandjsak", (int)(Math.random() * 1000000));
                mainActivity.dbRepository.fileMetaDataDao.insertFileMetaData(metaData);
            });
            navController.navigate(R.id.receiveFragment);
        }
    }
}