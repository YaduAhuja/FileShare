package com.yaindustries.fileshare.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.yaindustries.fileshare.MainActivity;
import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.utilities.ConnectionHelper;
import com.yaindustries.fileshare.utilities.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class TransferFragment extends Fragment {

    private NavController navController;
    private MainActivity activity;
    private TextView fileDetailsTextView;
    private ProgressBar fileTransferProgressBar;
    private ConnectionHelper connectionHelper;

    public TransferFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfer, container, false);
        activity = (MainActivity) getActivity();
        connectionHelper = activity.connectionHelper;
        initializeViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (activity.sending)
            CompletableFuture.runAsync(this::startSending);
        else
            CompletableFuture.runAsync(this::startReceiving);
//        CompletableFuture.runAsync(this::checkStorage);
    }


    private void initializeViews(@NonNull View view) {
        fileDetailsTextView = view.findViewById(R.id.textViewTransferFileDetails);
        fileTransferProgressBar = view.findViewById(R.id.fileTransferProgressBar);
    }

    private void startSending() {
        for (var metaData : activity.sendFilesQueue.keySet()) {
            try {
                activity.runOnUiThread(() -> fileDetailsTextView.setText("Now Transferring : " + metaData.name));
                connectionHelper.pushDataToSocket(activity, fileTransferProgressBar, activity.sendFilesQueue.get(metaData));
            } catch (IOException e) {
                activity.runOnUiThread(() -> Utils.showToast(getContext(), "Error in Transferring File " + metaData.name));
            }
        }

        activity.runOnUiThread(() -> Utils.showToast(getContext(), "File Transfer Complete"));
    }

    private void startReceiving() {
        try {
            connectionHelper.readDataFromSocket(activity, fileTransferProgressBar, fileDetailsTextView);
        } catch (IOException e) {
            activity.runOnUiThread(() -> Utils.showToast(getContext(), "Error in Receiving File "));
        }
    }

    private void checkStorage() {
        for (var metaData : activity.sendFilesQueue.keySet()) {
            try {
                var inputStream = activity.getContentResolver().openInputStream(activity.sendFilesQueue.get(metaData));
                File f = new File(Utils.getDataDirectoryPath() + metaData.name);
                f.getParentFile().mkdirs();
                f.createNewFile();
                var outputStream = new FileOutputStream(f);
                Utils.copyFile(activity, inputStream, outputStream, fileTransferProgressBar, metaData.size);
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                Log.d("Check Storage", "" + e);
                getActivity().runOnUiThread(() -> Utils.showToast(getActivity(), e + ""));
            }
        }
    }
}