package com.YAIndustries.fileshare.ui;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.YAIndustries.fileshare.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceListViewAdapter extends RecyclerView.Adapter<DeviceListViewAdapter.ViewHolder> {
    private Context context;
    private List<WifiP2pDevice> devices;

    public DeviceListViewAdapter(Context context, List<WifiP2pDevice> devices) {
        this.context = context;
        this.devices = devices;
    }

    @NonNull
    @Override
    public DeviceListViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        var view = inflater.inflate(R.layout.device_list_view_row, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListViewAdapter.ViewHolder holder, int position) {
        holder.textView.setText(devices.get(position).deviceName);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(@NonNull View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            textView = (TextView) view.findViewById(R.id.device_list_row_tv);
        }

        public TextView getTextView() {
            return textView;
        }
    }
}
