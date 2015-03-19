package com.brucegiese.perfectposture;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import android.provider.Settings.System;

public class PostureData extends ContentProvider {

    static final String TAG = "ProviderDemo";

    static final String AUTHORITY = "content://com.marakana.android.lifecycle.providerdemo";
    public static final Uri CONTENT_URI = Uri.parse(AUTHORITY);
    static final String SINGLE_RECORD_MIME_TYPE = "vnd.android.cursor.item/vnd.marakana.android.lifecycle.status";
    static final String MULTIPLE_RECORDS_MIME_TYPE = "vnd.android.cursor.dir/vnd.marakana.android.lifecycle.status";

    // TODO: Add global definitions for the database here

    public final class Contract {
        public Contract() {
        }

        public abstract class PostureSamples implements BaseColumns {
            public static final String TABLE_NAME = "posturesamples";
            public static final String COLUMN_NAME_SAMPLE = "sample";
            public static final String COLUMN_NAME_TIME = "secondssinceepoch";
        }
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        return true;
    }

    @Override
    public String getType(Uri uri) {
        String ret = getContext().getContentResolver().getType(System.CONTENT_URI);
        Log.d(TAG, "getType returning: " + ret);
        return ret;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "insert uri: " + uri.toString());
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.d(TAG, "update uri: " + uri.toString());
        return 0;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "delete uri: " + uri.toString());
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query with uri: " + uri.toString());
        return null;
    }

}