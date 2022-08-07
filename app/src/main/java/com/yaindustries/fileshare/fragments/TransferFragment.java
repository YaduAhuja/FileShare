package com.yaindustries.fileshare.fragments;

import android.os.Bundle;
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
import com.yaindustries.fileshare.models.TransferUiElements;
import com.yaindustries.fileshare.utilities.ConnectionHelper;
import com.yaindustries.fileshare.utilities.Utils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class TransferFragment extends Fragment {

    private NavController navController;
    private MainActivity mainActivity;
    private TextView fileDetailsTextView, progressDetailsTextView;
    private ProgressBar fileTransferProgressBar;
    private ConnectionHelper connectionHelper;

    public TransferFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfer, container, false);
        initialize(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        if (mainActivity.sending)
            CompletableFuture.runAsync(this::startSending);
        else
            CompletableFuture.runAsync(this::startReceiving);
    }


    private void initialize(@NonNull View view) {
        //Data Members
        mainActivity = (MainActivity) getActivity();
        connectionHelper = mainActivity.connectionHelper;
        navController = Navigation.findNavController(view);

        //Ui Elements
        fileDetailsTextView = view.findViewById(R.id.textViewTransferFileDetails);
        fileTransferProgressBar = view.findViewById(R.id.fileTransferProgressBar);
        progressDetailsTextView = view.findViewById(R.id.textViewProgressBar);
    }

    private void startSending() {
        for (var pair : mainActivity.sendFilesQueue) {
            try {
                mainActivity.runOnUiThread(() -> fileDetailsTextView.setText("Now Transferring : " + pair.first.name));
                var uiElements = new TransferUiElements(mainActivity, fileTransferProgressBar, progressDetailsTextView);
                connectionHelper.pushDataToSocket(uiElements, pair.second);
                mainActivity.dbRepository.fileMetaDataDao.insertFileMetaData(pair.first);
            } catch (IOException e) {
                mainActivity.runOnUiThread(() -> Utils.showToast(getContext(), "Error in Transferring File " + pair.first.name));
            }
        }

        mainActivity.runOnUiThread(() -> Utils.showToast(getContext(), "File Transfer Complete"));
    }

    private void startReceiving() {
        try {
            var uiElements = new TransferUiElements(mainActivity, fileTransferProgressBar, progressDetailsTextView);
            connectionHelper.readDataFromSocket(uiElements, fileDetailsTextView);
        } catch (IOException e) {
            mainActivity.runOnUiThread(() -> Utils.showToast(getContext(), "Error in Receiving File "));
        }
    }
}