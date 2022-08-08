package com.yaindustries.fileshare.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.yaindustries.fileshare.MainActivity;
import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.models.Pair;
import com.yaindustries.fileshare.ui.FileFragmentRecyclerViewAdapter;
import com.yaindustries.fileshare.utilities.Utils;

public class FileFragment extends Fragment implements View.OnClickListener {
    private final ActivityResultLauncher<String> filePicker;
    private MainActivity activity;
    private NavController navController;
    private Button selectFileButton, confirmButton;
    private ImageButton backButton;
    private RecyclerView fileRecyclerView;
    private FileFragmentRecyclerViewAdapter adapter;
    private TextView alternateTv;

    public FileFragment() {
        filePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (Uri result) -> {
                    if (result == null)
                        return;
                    var queryResult = getContext().getContentResolver().query(result, null, null, null, null);
                    var metaData = Utils.getFileMetaDataFromCursor(queryResult);
                    activity.sendFilesQueue.add(new Pair<>(metaData, result));
                    adapter.notifyItemInserted(activity.sendFilesQueue.size() - 1);
                    syncViews();
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        var view = inflater.inflate(R.layout.fragment_file, container, false);
        initialize(view);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        navController = Navigation.findNavController(view);
    }

    private void initialize(View view) {
        //Data Members
        activity = (MainActivity) getActivity();

        //Ui Elements
        selectFileButton = view.findViewById(R.id.selectFileButton);
        confirmButton = view.findViewById(R.id.fileConfirmButton);
        fileRecyclerView = view.findViewById(R.id.fileRecyclerView);
        backButton = view.findViewById(R.id.fileFragmentBackButton);
        alternateTv = view.findViewById(R.id.fileFragmentAlternateTv);
        adapter = new FileFragmentRecyclerViewAdapter(activity.sendFilesQueue, (position) -> {
            activity.sendFilesQueue.remove(position);
            adapter.notifyItemRemoved(position);
            syncViews();
        });
        fileRecyclerView.setAdapter(adapter);

        selectFileButton.setOnClickListener(this);
        confirmButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == selectFileButton)
            filePicker.launch("*/*");
        if (v == confirmButton)
            navController.navigate(R.id.sendFragment);
        if (v == backButton)
            navController.navigateUp();
    }

    private void syncViews() {
        if (activity.sendFilesQueue.isEmpty()) {
            fileRecyclerView.setVisibility(View.INVISIBLE);
            alternateTv.setVisibility(View.VISIBLE);
        } else {
            alternateTv.setVisibility(View.INVISIBLE);
            fileRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}