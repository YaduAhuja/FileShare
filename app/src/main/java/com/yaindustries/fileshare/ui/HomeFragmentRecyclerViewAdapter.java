package com.yaindustries.fileshare.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.models.FileMetaData;
import com.yaindustries.fileshare.utilities.Utils;

import java.util.List;

public class HomeFragmentRecyclerViewAdapter extends RecyclerView.Adapter<HomeFragmentRecyclerViewAdapter.ViewHolder> {
    private final List<FileMetaData> files;

    public HomeFragmentRecyclerViewAdapter(@NonNull List<FileMetaData> files) {
        this.files = files;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.heading_detail_row_recycler_view, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        var metaData = files.get(position);
        holder.tvFileName.setText(metaData.name);
        holder.tvFileSize.setText(Utils.getFileSizeInMBs(metaData.size));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }


    protected static class ViewHolder extends RecyclerView.ViewHolder {
        protected final TextView tvFileName, tvFileSize;

        public ViewHolder(@NonNull View view) {
            super(view);
            tvFileName = view.findViewById(R.id.tvDetailRowHeading);
            tvFileSize = view.findViewById(R.id.tvDetailRowData);
        }
    }
}
