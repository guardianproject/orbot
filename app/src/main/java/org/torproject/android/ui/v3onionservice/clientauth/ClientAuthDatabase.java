package org.torproject.android.ui.v3onionservice.clientauth;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ClientAuthDatabase extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "v3_client_auths";
    private static final int DATABASE_VERSION = 1;

    private static final String V3_AUTHS_CREATE_SQL =
            "CREATE TABLE " + DATABASE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "domain TEXT, " +
                    "hash TEXT, " +
                    "enabled INTEGER DEFAULT 1);";

    ClientAuthDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(V3_AUTHS_CREATE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
