package org.torproject.android.ui.hiddenservices.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.torproject.android.ui.hiddenservices.database.HSDatabase;


public class CookieContentProvider extends ContentProvider {
    public static final String[] PROJECTION = new String[]{
            ClientCookie._ID,
            ClientCookie.DOMAIN,
            ClientCookie.AUTH_COOKIE_VALUE,
            ClientCookie.ENABLED
    };
    private static final String AUTH = "org.torproject.android.ui.hiddenservices.providers.cookie";
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTH + "/cookie");
    //UriMatcher
    private static final int COOKIES = 1;
    private static final int COOKIE_ID = 2;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTH, "hs", COOKIES);
        uriMatcher.addURI(AUTH, "hs/#", COOKIE_ID);
    }

    private HSDatabase mServervices;
    private Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();
        mServervices = new HSDatabase(mContext);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String where = selection;
        if (uriMatcher.match(uri) == COOKIE_ID) {
            where = "_id=" + uri.getLastPathSegment();
        }

        SQLiteDatabase db = mServervices.getReadableDatabase();

        return db.query(HSDatabase.HS_CLIENT_COOKIE_TABLE_NAME, projection, where,
                selectionArgs, null, null, sortOrder);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriMatcher.match(uri);

        switch (match) {
            case COOKIES:
                return "vnd.android.cursor.dir/vnd.torproject.cookies";
            case COOKIE_ID:
                return "vnd.android.cursor.item/vnd.torproject.cookie";
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long regId;

        SQLiteDatabase db = mServervices.getWritableDatabase();

        regId = db.insert(HSDatabase.HS_CLIENT_COOKIE_TABLE_NAME, null, values);

        mContext.getContentResolver().notifyChange(CONTENT_URI, null);

        return ContentUris.withAppendedId(CONTENT_URI, regId);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        String where = selection;
        if (uriMatcher.match(uri) == COOKIE_ID) {
            where = "_id=" + uri.getLastPathSegment();
        }

        SQLiteDatabase db = mServervices.getWritableDatabase();

        int rows = db.delete(HSDatabase.HS_CLIENT_COOKIE_TABLE_NAME, where, selectionArgs);

        mContext.getContentResolver().notifyChange(CONTENT_URI, null);

        return rows;

    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mServervices.getWritableDatabase();

        String where = selection;
        if (uriMatcher.match(uri) == COOKIE_ID) {
            where = "_id=" + uri.getLastPathSegment();
        }

        int rows = db.update(HSDatabase.HS_CLIENT_COOKIE_TABLE_NAME, values, where, null);
        mContext.getContentResolver().notifyChange(CONTENT_URI, null);

        return rows;
    }

    public static final class ClientCookie implements BaseColumns {
        public static final String DOMAIN = "domain";
        public static final String AUTH_COOKIE_VALUE = "auth_cookie_value";
        public static final String ENABLED = "enabled";

        private ClientCookie() {
        }
    }
}
