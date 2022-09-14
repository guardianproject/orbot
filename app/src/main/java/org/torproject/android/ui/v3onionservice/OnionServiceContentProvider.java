package org.torproject.android.ui.v3onionservice;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.torproject.android.BuildConfig;

public class OnionServiceContentProvider extends ContentProvider {

    public static final String[] PROJECTION = {
            OnionService._ID,
            OnionService.NAME,
            OnionService.PORT,
            OnionService.DOMAIN,
            OnionService.ONION_PORT,
            OnionService.CREATED_BY_USER,
            OnionService.ENABLED,
            OnionService.PATH
    };

    private static final int ONIONS = 1, ONION_ID = 2;
    private static final String AUTH = BuildConfig.APPLICATION_ID + ".ui.v3onionservice";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTH + "/v3");
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTH, "v3", ONIONS);
        uriMatcher.addURI(AUTH, "v3/#", ONION_ID);
    }

    private OnionServiceDatabase mDatabase;

    @Override
    public boolean onCreate() {
        mDatabase = new OnionServiceDatabase(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (uriMatcher.match(uri) == ONION_ID)
            selection = "_id=" + uri.getLastPathSegment();
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        return db.query(OnionServiceDatabase.ONION_SERVICE_TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
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
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        long regId = db.insert(OnionServiceDatabase.ONION_SERVICE_TABLE_NAME, null, values);
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return ContentUris.withAppendedId(CONTENT_URI, regId);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (uriMatcher.match(uri) == ONION_ID)
            selection = "_id=" + uri.getLastPathSegment();
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        int rows = db.delete(OnionServiceDatabase.ONION_SERVICE_TABLE_NAME, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return rows;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mDatabase.getWritableDatabase();
        if (uriMatcher.match(uri) == ONION_ID)
            selection = "_id=" + uri.getLastPathSegment();
        int rows = db.update(OnionServiceDatabase.ONION_SERVICE_TABLE_NAME, values, selection, null);
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return rows;
    }

    public static final class OnionService implements BaseColumns {
        public static final String NAME = "name";
        public static final String PORT = "port";
        public static final String ONION_PORT = "onion_port";
        public static final String DOMAIN = "domain";
        public static final String CREATED_BY_USER = "created_by_user";
        public static final String ENABLED = "enabled";
        public static final String PATH = "filepath";
        private OnionService() { // no-op
        }
    }

}
