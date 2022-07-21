package com.yaindustries.fileshare.ui;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.interfaces.RecyclerViewTaskInvoker;

import java.util.List;

public class SendFragmentRecyclerViewAdapter extends RecyclerView.Adapter<SendFragmentRecyclerViewAdapter.ViewHolder> {
    private final List<WifiP2pDevice> deviceList;
    private final RecyclerViewTaskInvoker taskInvoker;

    public SendFragmentRecyclerViewAdapter(@NonNull List<WifiP2pDevice> deviceList, @NonNull RecyclerViewTaskInvoker taskInvoker) {
        this.deviceList = deviceList;
        this.taskInvoker = taskInvoker;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.heading_detail_row_recycler_view, parent, false);
        return new ViewHolder(view, taskInvoker);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        var device = deviceList.get(position);
        holder.tvHeading.setText(String.format("Device Name : %s", device.deviceName));
        holder.tvData.setText(String.format("Address : %s", device.deviceAddress));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView tvHeading;
        private final TextView tvData;
        private final RecyclerViewTaskInvoker taskInvoker;

        public ViewHolder(@NonNull View view, @NonNull RecyclerViewTaskInvoker taskInvoker) {
            super(view);
            this.taskInvoker = taskInvoker;

            tvHeading = view.findViewById(R.id.tvDetailRowHeading);
            tvData = view.findViewById(R.id.tvDetailRowData);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            taskInvoker.onClickTask(position);
        }
    }
}
