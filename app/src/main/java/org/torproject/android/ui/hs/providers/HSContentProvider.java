package org.torproject.android.ui.hs.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;

import org.torproject.android.ui.hs.database.HSDatabase;


public class HSContentProvider extends ContentProvider {
    private static final String AUTH = "org.torproject.android.ui.hs.providers";
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTH + "/hs");
    //UriMatcher
    private static final int ONIONS = 1;
    private static final int ONION_ID = 2;

    private static final UriMatcher uriMatcher;

    //Inicializamos el UriMatcher
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTH, "hs", ONIONS);
        uriMatcher.addURI(AUTH, "hs/#", ONION_ID);
    }

    private HSDatabase mServerDB;
    private Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        mServerDB = new HSDatabase(mContext);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        //Si es una consulta a un ID concreto construimos el WHERE
        String where = selection;
        if (uriMatcher.match(uri) == ONION_ID) {
            where = "_id=" + uri.getLastPathSegment();
        }

        SQLiteDatabase db = mServerDB.getReadableDatabase();

        return db.query(HSDatabase.HS_DATA_TABLE_NAME, projection, where,
                selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        int match = uriMatcher.match(uri);

        switch (match) {
            case ONIONS:
                return "vnd.android.cursor.dir/vnd.torproject.onions";
            case ONION_ID:
                return "vnd.android.cursor.item/vnd.torproject.onion";
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long regId;

        SQLiteDatabase db = mServerDB.getWritableDatabase();

        regId = db.insert(HSDatabase.HS_DATA_TABLE_NAME, null, values);

        mContext.getContentResolver().notifyChange(CONTENT_URI, null);

        return ContentUris.withAppendedId(CONTENT_URI, regId);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        //Si es una consulta a un ID concreto construimos el WHERE
        String where = selection;
        if (uriMatcher.match(uri) == ONION_ID) {
            where = "_id=" + uri.getLastPathSegment();
        }

        SQLiteDatabase db = mServerDB.getWritableDatabase();

        Integer rows = db.delete(HSDatabase.HS_DATA_TABLE_NAME, where, selectionArgs);

        mContext.getContentResolver().notifyChange(CONTENT_URI, null);

        return rows;

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mServerDB.getWritableDatabase();

        String where = selection;
        if (uriMatcher.match(uri) == ONION_ID) {
            where = "_id=" + uri.getLastPathSegment();
        }

        Integer rows = db.update(HSDatabase.HS_DATA_TABLE_NAME, values, where, null);
        mContext.getContentResolver().notifyChange(CONTENT_URI, null);

        return rows;
    }

    public static final class HiddenService implements BaseColumns {
        //Nombres de columnas
        public static final String NAME = "name";
        public static final String PORT = "port";
        public static final String ONION_PORT = "onion_port";
        public static final String DOMAIN = "domain";

        private HiddenService() {
        }
    }
}
