package com.YAIndustries.fileshare.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.YAIndustries.fileshare.R;

public class DiscoverPeersFragment extends Fragment {
    public static DiscoverPeersFragment newInstance() {
        return new DiscoverPeersFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover_peers, container, false);
    }
}