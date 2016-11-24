package org.torproject.android.ui.hiddenservices.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HSDatabase extends SQLiteOpenHelper {

    public static final String HS_DATA_TABLE_NAME = "hs_data";
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "hidden_services";
    private static final String HS_DATA_TABLE_CREATE =
            "CREATE TABLE " + HS_DATA_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "domain TEXT, " +
                    "onion_port INTEGER, " +
                    "created_by_user INTEGER DEFAULT 0, " +
                    "port INTEGER);";

    public HSDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(HS_DATA_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}

