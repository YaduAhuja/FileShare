package com.yaindustries.fileshare.models;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TransferUiElements {
    public AppCompatActivity activity;
    public ProgressBar progressBar;
    public TextView progressTv;

    public TransferUiElements(AppCompatActivity activity, ProgressBar progressBar, TextView textView) {
        this.activity = activity;
        this.progressBar = progressBar;
        progressTv = textView;
    }
}
