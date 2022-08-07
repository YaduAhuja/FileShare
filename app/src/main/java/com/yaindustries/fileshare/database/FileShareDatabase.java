package com.yaindustries.fileshare.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.yaindustries.fileshare.models.FileMetaData;

@Database(entities = {FileMetaData.class}, version = 1)
public abstract class FileShareDatabase extends RoomDatabase {
    public abstract FileMetaDataDao fileMetaDataDao();
    private static volatile FileShareDatabase INSTANCE;

    static FileShareDatabase getDatabase(final Context context) {
        if(INSTANCE == null) {
            synchronized (FileShareDatabase.class) {
                if(INSTANCE == null)
                    INSTANCE = Room
                            .databaseBuilder(context.getApplicationContext(), FileShareDatabase.class, "FileShareDatabase")
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .fallbackToDestructiveMigration()
                            .build();
            }
        }
        return INSTANCE;
    }
}
