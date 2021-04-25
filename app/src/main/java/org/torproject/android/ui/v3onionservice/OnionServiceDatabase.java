package org.torproject.android.ui.v3onionservice;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class OnionServiceDatabase extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "onion_service",
            ONION_SERVICE_TABLE_NAME = "onion_services";
    private static final int DATABASE_VERSION = 2;

    private static final String ONION_SERVICES_CREATE_SQL =
            "CREATE TABLE " + ONION_SERVICE_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "domain TEXT, " +
                    "onion_port INTEGER, " +
                    "created_by_user INTEGER DEFAULT 0, " +
                    "enabled INTEGER DEFAULT 1, " +
                    "port INTEGER, " +
                    "filepath TEXT);";

    OnionServiceDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ONION_SERVICES_CREATE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            db.execSQL("ALTER TABLE " + ONION_SERVICE_TABLE_NAME + " ADD COLUMN filepath text");
        }
    }


}
