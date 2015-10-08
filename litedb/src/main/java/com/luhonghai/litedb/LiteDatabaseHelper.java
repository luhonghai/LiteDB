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

package com.luhonghai.litedb;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.luhonghai.litedb.annotation.AnnotationHelper;
import com.luhonghai.litedb.annotation.LiteColumn;
import com.luhonghai.litedb.annotation.LiteDatabase;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;
import com.luhonghai.litedb.exception.LiteDatabaseException;
import com.luhonghai.litedb.exception.UnsupportedFieldType;
import com.luhonghai.litedb.meta.LiteTableMeta;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by luhonghai on 07/09/15.
 */
public abstract class LiteDatabaseHelper {

    /**
     * Listener for watch verify function to create or upgrade database
     */
    public interface DatabaseListener {
        /**
         *  Before verify all tables when database create
         * @param db
         */
        void onBeforeDatabaseCreate(SQLiteDatabase db);
        /**
         *  After verify all tables when database create
         * @param db
         */
        void onAfterDatabaseCreate(SQLiteDatabase db);
        /**
         *  Before verify all tables when database upgrade
         * @param db
         */
        void onBeforeDatabaseUpgrade(final SQLiteDatabase db, final int oldVersion,
                                     final int newVersion);
        /**
         *  After verify all tables when database upgrade
         * @param db
         */
        void onAfterDatabaseUpgrade(final SQLiteDatabase db, final int oldVersion,
                                    final int newVersion);

        /**
         * Catch exception from verify database method
         * @param db
         * @param message
         * @param throwable
         */
        void onError(SQLiteDatabase db, String message, Throwable throwable);
    }

    private static final String TAG = "LiteDB";

    /** Variable to hold the database instance. */
    private SQLiteDatabase mDB;

    /** Database open/upgrade helper. */
    private DatabaseHelper mOpenHelper;

    /** The context within which to work. */
    private final Context mContext;
    /**
     * The exchange query object
     */
    private final LiteQuery liteQuery;
    /**
     * A helper class is used to manage database.
     */
    private final Map<String, LiteTableMeta> tableMetaData
            = new ConcurrentHashMap<String, LiteTableMeta>();

    /**
     * A helper class is used to manage database.
     */
    private final Map<String, AnnotationHelper> annotationHelpers
            = new ConcurrentHashMap<String, AnnotationHelper>();

    private boolean useClassSchema;

    public boolean isUseClassSchema() {
        return useClassSchema;
    }

    public static final class DatabaseHelper extends SQLiteOpenHelper {

        private final Class[] tableClasses;

        private DatabaseListener databaseListener;

        /**
         * Constructor
         * @param context application context
         * @param databaseName the name of database
         * @param databaseVersion version of database
         * @param tableClasses list of table classes
         */
        public DatabaseHelper(final Context context,
                              String databaseName,
                              int databaseVersion,
                              Class[] tableClasses) {
            super(context, databaseName, null, databaseVersion);
            this.tableClasses = tableClasses;
        }

        /**
         * Constructor
         * @param context application context
         * @param databaseName the name of database
         * @param databaseVersion version of database
         * @param tableClasses list of table classes
         * @param databaseListener listen for database update or create
         */
        public DatabaseHelper(final Context context,
                              String databaseName,
                              int databaseVersion,
                              Class[] tableClasses,
                              DatabaseListener databaseListener) {
            this(context, databaseName, databaseVersion, tableClasses);
            setDatabaseListener(databaseListener);
        }

        @Override
        public void onCreate(final SQLiteDatabase db) {
            if (databaseListener != null) databaseListener.onBeforeDatabaseCreate(db);
            verifyDatabase(db);
            if (databaseListener != null) databaseListener.onAfterDatabaseCreate(db);
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
                              final int newVersion) {
            if (databaseListener != null) databaseListener.onBeforeDatabaseUpgrade(db, oldVersion, newVersion);
            verifyDatabase(db);
            if (databaseListener != null) databaseListener.onAfterDatabaseUpgrade(db, oldVersion, newVersion);
        }

        /**
         * To verify all tables of database
         * Create new table or new column if needed
         * @param db
         */
        private void verifyDatabase(final SQLiteDatabase db) {
            // Loop all table classes that is defined on LiteDatabase annotation
            for (Class clazz : tableClasses) {
                try {
                    AnnotationHelper annotationHelper = new AnnotationHelper(clazz);
                    String tableName = annotationHelper.getTableName();
                    if (isTableExists(db, tableName)) {
                        // Table is exists. Check all fields to add column if needed
                        for (Field field : clazz.getDeclaredFields()) {
                            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
                            if (liteColumn != null) {
                                if (!isColumnExists(db, tableName, annotationHelper.getColumnName(field))) {
                                    // Column is not exists. Try to add new
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
                    if (databaseListener != null) databaseListener.onError(db,
                            "Could not verify table " + clazz.getName(),
                            e);
                }
            }
        }

        /**
         * * Check if table is exists on database
         * @param db
         * @param table
         * @return true if table is exist
         */
        private boolean isTableExists(final SQLiteDatabase db, final String table) {
            Cursor cursor = db.rawQuery(
                    "select DISTINCT [tbl_name] from [sqlite_master] where [tbl_name] = ?",
                    new String[] {table});
            if (cursor != null) {
                try {
                    return cursor.getCount() > 0;
                } finally {
                    cursor.close();
                }
            }
            return false;
        }

        /**
         * Check if column is exists on table
         * @param db
         * @param table
         * @param column
         * @return true if column is exist
         */
        private boolean isColumnExists(final SQLiteDatabase db, final String table, final String column) {
            Cursor mCursor = null;
            try {
                mCursor = db.rawQuery("SELECT * FROM [" + table + "] LIMIT 0", null);
                return mCursor.getColumnIndex(column) != -1;
            } catch (Exception ex) {
                // Something went wrong. Missing the database? The table?
                Log.e(TAG, "When checking whether a column exists in the table", ex);
                return false;
            } finally {
                if (mCursor != null) mCursor.close();
            }
        }

        public void setDatabaseListener(DatabaseListener databaseListener) {
            this.databaseListener = databaseListener;
        }

        public Class[] getTableClass() {
            return tableClasses;
        }
    }

    /**
     * Construct database service with context of the application.
     *
     * @param context
     *            the Context within which to work
     */
    public LiteDatabaseHelper(final Context context) throws AnnotationNotFound, InvalidAnnotationData {
        LiteDatabase liteDatabase = this.getClass().getAnnotation(LiteDatabase.class);
        if (liteDatabase == null)
            throw new AnnotationNotFound(LiteDatabase.class);
        mContext = context;
        useClassSchema = liteDatabase.useClassSchema();
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
        liteQuery = new LiteQuery(this);
    }

    /**
     * Construct database service with context of the application.
     *
     * @param context
     *            the Context within which to work
     * @param databaseListener
     *              the listener for database change
     * @throws AnnotationNotFound
     * @throws InvalidAnnotationData
     */
    public LiteDatabaseHelper(final Context context, DatabaseListener databaseListener)
            throws AnnotationNotFound, InvalidAnnotationData {
        this(context);
        setDatabaseListener(databaseListener);
    }

    /**
     * Get context.
     * @return current context
     */
    public final Context getContext() {
        return mContext;
    }

    /**
     * Getter of lite query object
     * @return lite query object
     */
    public LiteQuery getLiteQuery() {
        return liteQuery;
    }

    /**
     *  Get database connection.
     * @return SQLite database object
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

    /**
     * Set database listener
     * @param databaseListener
     */
    public void setDatabaseListener(DatabaseListener databaseListener) {
        mOpenHelper.setDatabaseListener(databaseListener);
    }

    /**
     * Check if table is exists
     * @param table name of table to check (SQLite table name)
     * @return true if table is exist
     */
    public boolean isTableExists(String table) {
        return mOpenHelper.isTableExists(mDB, table);
    }

    /**
     * Check if column of table is exists
     * @param table name of table to check (SQLite table name)
     * @param column column to check (SQLite column name)
     * @return true if column is exist if table
     */
    public boolean isColumnExists(String table, String column) {
        return mOpenHelper.isColumnExists(mDB, table, column);
    }

    /**
     * Get annotation helper
     * @param clazz
     * @return annotation helper
     */
    public AnnotationHelper getAnnotationHelper(Class<?> clazz) {
        if (!annotationHelpers.containsKey(clazz.getName())) {
            annotationHelpers.put(clazz.getName(), new AnnotationHelper(clazz));
        }
        return annotationHelpers.get(clazz.getName());
    }

    /**
     * Get table meta data
     * @param clazz
     * @return
     * @throws LiteDatabaseException
     */
    public LiteTableMeta getTableMeta(Class<?> clazz) throws LiteDatabaseException {
        if (!tableMetaData.containsKey(clazz.getName())) {
            final AnnotationHelper annotationHelper = getAnnotationHelper(clazz);
            try {
                tableMetaData.put(clazz.getName(), annotationHelper.generateTableMeta());
            } catch (AnnotationNotFound | UnsupportedFieldType | InvalidAnnotationData e) {
                throw new LiteDatabaseException("Could not get table meta data",e);
            }
        }
        return tableMetaData.get(clazz.getName());
    }

    /**
     * Get all defined table classes
     * @return all table classes
     */
    public Class[] getTableClasses() {
        return mOpenHelper.getTableClass();
    }

    /**
     * Try to load all table meta data
     * @return table meta data map
     * @throws LiteDatabaseException
     */
    public Map<String, LiteTableMeta> getTableMetaMap() throws LiteDatabaseException {
        Class<?>[] tableClasses = getTableClasses();
        if (tableClasses != null && tableMetaData.size() != tableClasses.length) {
            for (Class<?> clazz : tableClasses) {
                getTableMeta(clazz);
            }
        }
        return tableMetaData;
    }
}
