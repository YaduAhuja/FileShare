package com.yaindustries.fileshare.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class FileMetaData {
    @PrimaryKey(autoGenerate = true)
    @JsonIgnore
    public int id;

    @ColumnInfo(name = "fileName")
    public String name;

    @ColumnInfo(name = "fileSize")
    public long size;

    public FileMetaData() {}
    public FileMetaData(String name, long size) {
        this.name = name;
        this.size = size;
    }
}
