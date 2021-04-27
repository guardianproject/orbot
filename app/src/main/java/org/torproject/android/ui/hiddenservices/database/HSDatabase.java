package org.torproject.android.ui.hiddenservices.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HSDatabase extends SQLiteOpenHelper {

    public static final String HS_DATA_TABLE_NAME = "hs_data";
    public static final String HS_CLIENT_COOKIE_TABLE_NAME = "hs_client_cookie";
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "hidden_services";
    private static final String HS_DATA_TABLE_CREATE =
            "CREATE TABLE " + HS_DATA_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "domain TEXT, " +
                    "onion_port INTEGER, " +
                    "auth_cookie INTEGER DEFAULT 0, " +
                    "auth_cookie_value TEXT, " +
                    "created_by_user INTEGER DEFAULT 0, " +
                    "enabled INTEGER DEFAULT 1, " +
                    "port INTEGER, " +
                    "filepath TEXT);";

    private static final String HS_CLIENT_COOKIE_TABLE_CREATE =
            "CREATE TABLE " + HS_CLIENT_COOKIE_TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "domain TEXT, " +
                    "auth_cookie_value TEXT, " +
                    "enabled INTEGER DEFAULT 1);";

    public HSDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(HS_DATA_TABLE_CREATE);
        db.execSQL(HS_CLIENT_COOKIE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            db.execSQL("ALTER TABLE " + HS_DATA_TABLE_NAME + " ADD COLUMN filepath TEXT");
        }
    }
}