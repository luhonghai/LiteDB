/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015. Hai Lu @ luhonghai.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 */

package com.halosolutions.litedb;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.halosolutions.litedb.annotation.AnnotationHelper;
import com.halosolutions.litedb.annotation.LiteColumn;
import com.halosolutions.litedb.annotation.LiteDatabase;
import com.halosolutions.litedb.exception.AnnotationNotFound;
import com.halosolutions.litedb.exception.InvalidAnnotationData;

import java.lang.reflect.Field;

/**
 * Created by luhonghai on 07/09/15.
 */
public abstract class LiteBaseDatabase {

    private static final String TAG = "LiteDB";

    /** Variable to hold the database instance. */
    private SQLiteDatabase mDB;
    /** Database open/upgrade helper. */
    private DatabaseHelper mOpenHelper;
    /** The context within which to work. */
    private final Context mContext;

    /**
     * A helper class is used to manage database.
     */
    public static final class DatabaseHelper extends SQLiteOpenHelper {

        private final Class[] tableClassess;

        /**
         * Constructor.
         *
         * @param context
         *            the context keeps this connection.
         */
        public DatabaseHelper(final Context context,
                              String databaseName,
                              int databaseVersion,
                              Class[] tableClasses) {
            super(context, databaseName, null, databaseVersion);
            this.tableClassess = tableClasses;
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            verifyDatabase(db);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                              final int newVersion) {
            verifyDatabase(db);
        }

        private void verifyDatabase(final SQLiteDatabase db) {
            for (Class clazz : tableClassess) {
                try {
                    AnnotationHelper annotationHelper = new AnnotationHelper(clazz);
                    String tableName = annotationHelper.getTableName();
                    if (isTableExists(db, tableName)) {
                        for (Field field : clazz.getDeclaredFields()) {
                            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
                            if (liteColumn != null) {
                                if (!isColumnExists(db, tableName, annotationHelper.getColumnName(field))) {
                                    String query = annotationHelper.getAddColumnQuery(field);
                                    Log.d(TAG, "Add new column. Query: " + query);
                                    db.execSQL(query);
                                }
                            }
                        }
                    } else {
                        String query = annotationHelper.getCreateTableQuery();
                        Log.d(TAG, "Create new table. Query: " + query);
                        db.execSQL(query);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Could not verify table " + clazz.getName(), e);
                }
            }
        }

        /**
         * * Check if table is exists on database
         * @param db
         * @param table
         * @return
         */
        private boolean isTableExists(final SQLiteDatabase db, final String table) {
            Cursor cursor = db.rawQuery(
                    "select DISTINCT tbl_name from sqlite_master where tbl_name = '"
                    + table + "'", null);
            if (cursor != null) {
                if (cursor.getCount()>0) {
                    cursor.close();
                    return true;
                }
                cursor.close();
            }
            return false;
        }

        /**
         * Check if column is exists on table
         * @param db
         * @param table
         * @param column
         * @return
         */
        private boolean isColumnExists(final SQLiteDatabase db, final String table, final String column) {
            Cursor mCursor = null;
            try {
                mCursor = db.rawQuery("SELECT * FROM " + table + " LIMIT 0", null);
                return mCursor.getColumnIndex(column) != -1;
            } catch (Exception ex) {
                // Something went wrong. Missing the database? The table?
                Log.e(TAG, "When checking whether a column exists in the table", ex);
                return false;
            } finally {
                if (mCursor != null) mCursor.close();
            }
        }
    }

    /**
     * Construct database service with context of the application.
     *
     * @param context
     *            the Context within which to work
     */
    public LiteBaseDatabase(final Context context) throws AnnotationNotFound, InvalidAnnotationData {
        LiteDatabase liteDatabase = this.getClass().getAnnotation(LiteDatabase.class);
        if (liteDatabase == null)
            throw new AnnotationNotFound(LiteDatabase.class);
        mContext = context;
        Class[] tableClasses = liteDatabase.tables();
        if (tableClasses == null || tableClasses.length == 0)
            throw new InvalidAnnotationData("Require at least one table in database");
        String dbName = liteDatabase.name();
        if (dbName == null || dbName.length() == 0)
            dbName = this.getClass().getSimpleName();
        mOpenHelper = new DatabaseHelper(mContext,
                dbName,
                liteDatabase.version(),
                tableClasses);
    }

    /**
     * Get context.
     */
    public final Context getContext() {
        return mContext;
    }

    /**
     * Get database connection.
     */
    public final SQLiteDatabase getDatabase() {
        return mDB;
    }

    /** Open the database. Default is writeable */
    public final void open() {
        openWritable();
    }

    /** Open the database. */
    public final void openWritable() {
        if (mOpenHelper == null) {
            return;
        }
        mDB = mOpenHelper.getWritableDatabase();
    }

    /** Open the database. */
    public final void openReadable() {
        if (mOpenHelper == null) {
            return;
        }
        mDB = mOpenHelper.getReadableDatabase();
    }

    /** Close the database. */
    public final void close() {
        if (mDB != null) {
            mDB.close();
        }
    }

}
