package com.yaindustries.fileshare.database;

import android.content.Context;

public class DBRepository {
    private final FileShareDatabase database;
    public final FileMetaDataDao fileMetaDataDao;

    public DBRepository(Context context) {
        database = FileShareDatabase.getDatabase(context);
        fileMetaDataDao = database.fileMetaDataDao();
    }

    public void clear() {
        database.clearAllTables();
    }
}
