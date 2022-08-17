package com.yaindustries.fileshare.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.yaindustries.fileshare.models.FileMetaData;

import java.util.List;

@Dao
public interface FileMetaDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFileMetaData(FileMetaData...entities);

    @Delete
    void deleteFileMetaData(FileMetaData ...entities);

    @Query("SELECT * FROM FileMetaData WHERE ID=:id")
    FileMetaData findFileMetaDataById(int id);

    @Query("SELECT * FROM FileMetaData order by id desc LIMIT:limit")
    List<FileMetaData> getLastNFileMetaData(int limit);
}
