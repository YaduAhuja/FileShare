package com.yaindustries.fileshare.fragments;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.yaindustries.fileshare.MainActivity;
import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.models.FileMetaData;
import com.yaindustries.fileshare.models.TransferUiElements;
import com.yaindustries.fileshare.ui.TransferFragmentRecyclerViewAdapter;
import com.yaindustries.fileshare.utilities.ConnectionHelper;
import com.yaindustries.fileshare.utilities.Utils;
import com.yaindustries.fileshare.utilities.WifiP2pHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TransferFragment extends Fragment implements View.OnClickListener{

    private NavController navController;
    private MainActivity mainActivity;
    private TextView fileDetailsTextView, progressDetailsTextView, pageDescriptor;
    private ProgressBar fileTransferProgressBar;
    private ConnectionHelper connectionHelper;
    private Button disconnectBtn;
    private WifiP2pHelper wifiP2pHelper;
    private final List<FileMetaData> fileList = new ArrayList<>();
    private TransferFragmentRecyclerViewAdapter adapter;

    public TransferFragment() {}


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
        wifiP2pHelper = mainActivity.wifiP2pHelper;
        adapter = new TransferFragmentRecyclerViewAdapter(fileList);

        //Ui Elements
        fileDetailsTextView = view.findViewById(R.id.textViewTransferFileDetails);
        fileTransferProgressBar = view.findViewById(R.id.fileTransferProgressBar);
        progressDetailsTextView = view.findViewById(R.id.textViewProgressBar);
        pageDescriptor = view.findViewById(R.id.transferFragmentPageDescriptorTv);
        RecyclerView recyclerView = view.findViewById(R.id.transferFragmentFileRecyclerView);
        disconnectBtn = view.findViewById(R.id.transferFragmentDisconnectButton);

        recyclerView.setAdapter(adapter);
        disconnectBtn.setOnClickListener(this);

        if(!mainActivity.sending)
            pageDescriptor.setText(R.string.receiving);
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
            int prevSize = fileList.size();
            var uiElements = new TransferUiElements(mainActivity, fileTransferProgressBar, progressDetailsTextView);
            connectionHelper.readDataFromSocket(uiElements, fileDetailsTextView, fileList);
            if(prevSize != fileList.size())
                mainActivity.runOnUiThread(() -> adapter.notifyItemInserted(fileList.size()-1));
        } catch (IOException e) {
            mainActivity.runOnUiThread(() -> Utils.showToast(getContext(), "Error in Receiving File "));
        }
    }

    @Override
    public void onClick(View v) {
        if(v == disconnectBtn) {
            wifiP2pHelper.removeGroup(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Utils.showToast(getContext(), "Disconnected");
                }

                @Override
                public void onFailure(int i) {
                    Utils.showToast(getContext(), "Unable to Disconnect");
                }
            });
        }
    }
}