package com.yaindustries.fileshare.ui;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yaindustries.fileshare.R;
import com.yaindustries.fileshare.interfaces.RecyclerViewTaskInvoker;
import com.yaindustries.fileshare.models.FileMetaData;
import com.yaindustries.fileshare.models.Pair;

import java.util.List;

public class FileFragmentRecyclerViewAdapter extends RecyclerView.Adapter<FileFragmentRecyclerViewAdapter.ViewHolder> {
    private final List<Pair<FileMetaData, Uri>> files;
    private final RecyclerViewTaskInvoker taskInvoker;

    public FileFragmentRecyclerViewAdapter(@NonNull List<Pair<FileMetaData, Uri>> files, @NonNull RecyclerViewTaskInvoker taskInvoker) {
        this.files = files;
        this.taskInvoker = taskInvoker;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_file_recycler_view, parent, false);
        return new ViewHolder(view, taskInvoker);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        var metaData = files.get(position).first;
        holder.tvFileName.setText(String.format("Name : %s", metaData.name));
        var valueInMB = (metaData.size * 1.0) / (1L << 20);
        holder.tvFileSize.setText(String.format("Size : %.2f MB", valueInMB));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView tvFileName;
        private final TextView tvFileSize;
        private final RecyclerViewTaskInvoker taskInvoker;

        public ViewHolder(@NonNull View view, @NonNull RecyclerViewTaskInvoker taskInvoker) {
            super(view);

            this.taskInvoker = taskInvoker;
            tvFileName = view.findViewById(R.id.tvFileName);
            tvFileSize = view.findViewById(R.id.tvFileSize);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            taskInvoker.onClickTask(position);
        }
    }
}
