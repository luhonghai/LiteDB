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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.luhonghai.litedb.annotation.AnnotationHelper;
import com.luhonghai.litedb.annotation.LiteColumn;
import com.luhonghai.litedb.annotation.LiteTable;
import com.luhonghai.litedb.bulk.BulkInsert;
import com.luhonghai.litedb.bulk.BulkUpdate;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;
import com.luhonghai.litedb.exception.LiteDatabaseException;
import com.luhonghai.litedb.meta.LiteColumnMeta;
import com.luhonghai.litedb.meta.LiteTableMeta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by luhonghai on 07/09/15.
 */
public class LiteBaseDao<T> {
    /**
     *
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    /**
     *
     */
    private final AnnotationHelper annotationHelper;
    /**
     *
     */
    private final LiteDatabaseHelper databaseHelper;
    /**
     *
     */
    private final Class<T> tableClass;
    /**
     *
     */
    private final SimpleDateFormat sdfDateValue;

    /**
     * Constructor
     * @param databaseHelper
     * @param tableClass
     */
    public LiteBaseDao(LiteDatabaseHelper databaseHelper, Class<T> tableClass) {
        this.databaseHelper = databaseHelper;
        this.annotationHelper = databaseHelper.getAnnotationHelper(tableClass);
        this.tableClass = tableClass;
        this.sdfDateValue = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault());
        this.sdfDateValue.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Get lite query object
     * @return Lite query
     */
    public LiteQuery getLiteQuery() {
        return databaseHelper.getLiteQuery();
    }

    /**
     * Get current database object
     * @return SQLitedatabase
     */
    public SQLiteDatabase getDatabase() {
        return databaseHelper.getDatabase();
    }
    /**
     * Get annotation helper
     * @return annotation helper
     */
    public AnnotationHelper getAnnotationHelper() {
        return annotationHelper;
    }

    /**
     * Get database helper
     * @return database helper
     */
    public LiteDatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    /**
     * Open new database connection
     */
    public void open() {
        databaseHelper.open();
    }

    /**
     * Close database helper
     */
    public void close() {
        databaseHelper.close();
    }

    /**
     * Fill content values by object
     * @param object
     * @return content values with full data from object
     * @throws LiteDatabaseException
     */
    public ContentValues fillContentValues(final T object) throws LiteDatabaseException {
        ContentValues contentValues = new ContentValues();
        Iterator<String> fieldNames = getTableMeta().getColumns().keySet().iterator();
        while (fieldNames.hasNext()) {
            final String fieldName = fieldNames.next();
            final LiteColumnMeta columnMeta = getTableMeta().getColumns().get(fieldName);
            if (!columnMeta.isAutoincrement()) {
                putContentValues(contentValues,
                        fieldName,
                        object);
            }
        }
        return contentValues;
    }

    /**
     * Put field data to content values
     * @param contentValues
     * @param field
     * @param object
     * @throws LiteDatabaseException
     */
    public void putContentValues(final ContentValues contentValues, final Field field,
                                 final T object) throws LiteDatabaseException {
        putContentValues(contentValues, field.getName(), object);
    }

    /**
     * Put field data to content values
     * @param contentValues
     * @param fieldName
     * @param object
     * @throws LiteDatabaseException
     */
    public void putContentValues(final ContentValues contentValues, final String fieldName,
                                   final T object) throws LiteDatabaseException {
        Object fieldValue = null;
        final LiteColumnMeta columnMeta = getTableMeta().getColumns().get(fieldName);
        try {
            fieldValue = columnMeta.getValue(object);
        } catch (IllegalAccessException e) {
            throw new LiteDatabaseException("Could not get field value from object",e);
        }
        if (fieldValue == null) return;
        String key = columnMeta.getColumnName();
        switch (columnMeta.getFieldType()) {
            case LONG:
                contentValues.put(key, Long.valueOf(fieldValue.toString()));
                break;
            case STRING:
                contentValues.put(key, fieldValue.toString());
                break;
            case INTEGER:
                contentValues.put(key, Integer.valueOf(fieldValue.toString()));
                break;
            case FLOAT:
                contentValues.put(key, Float.valueOf(fieldValue.toString()));
                break;
            case BYTE:
                contentValues.put(key, Byte.valueOf(fieldValue.toString()));
                break;
            case SHORT:
                contentValues.put(key, Short.valueOf(fieldValue.toString()));
                break;
            case BOOLEAN:
                contentValues.put(key, Boolean.parseBoolean(fieldValue.toString()));
                break;
            case DOUBLE:
                contentValues.put(key, Double.valueOf(fieldValue.toString()));
                break;
            case DATE:
                switch (columnMeta.getDateColumnType()) {
                    case TEXT:
                        contentValues.put(key, sdfDateValue.format((Date) fieldValue));
                        break;
                    case INTEGER:
                        contentValues.put(key, ((Date) fieldValue).getTime());
                        break;
                    default:
                        throw new LiteDatabaseException("Invalid date column type " + columnMeta
                                .getDateColumnType().toString()
                                , new InvalidAnnotationData("Invalid dateColumnType"));
                }
                break;
            case BYTE_ARRAY:
            case SERIALIZABLE:
                ByteArrayOutputStream outputStream = null;
                ObjectOutputStream objectOutputStream = null;
                try {
                    outputStream = new ByteArrayOutputStream();
                    objectOutputStream = new ObjectOutputStream(
                            outputStream);
                    objectOutputStream.writeObject(fieldValue);
                    contentValues.put(key, outputStream.toByteArray());
                    objectOutputStream.flush();
                    outputStream.flush();
                } catch (Exception e) {
                    Log.e("", "", e);
                } finally {
                    if (objectOutputStream != null) {
                        try {
                            objectOutputStream.close();
                        } catch (Exception e) {}
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception e) {}
                    }
                }
                break;
        }
    }

    /**
     * Get content from specific field
     * @param cursor
     * @param field
     * @return value by special field from cursor
     * @throws LiteDatabaseException
     */
    public Object getValueFromCursor(Cursor cursor, Field field) throws LiteDatabaseException {
        return getValueFromCursor(cursor, field.getName());
    }
    /**
     * Get content from specific field
     * @param cursor
     * @param fieldName
     * @return value by special field from cursor
     * @throws LiteDatabaseException
     */
    public Object getValueFromCursor(Cursor cursor, String fieldName)
            throws LiteDatabaseException {
        LiteColumnMeta columnMeta = getTableMeta().getColumns().get(fieldName);
        Object value = null;
        int columnIndex;
        if (!"".equals(columnMeta.getAlias())) {
            columnIndex = cursor.getColumnIndex(columnMeta.getAlias());
        } else {
            columnIndex = cursor.getColumnIndex(columnMeta.getColumnName());
        }
        if (columnIndex == -1) return null;
        switch (columnMeta.getFieldType()) {
            case LONG:
                value = cursor.getLong(columnIndex);
                break;
            case STRING:
                value = cursor.getString(columnIndex);
                break;
            case INTEGER:
                value = cursor.getInt(columnIndex);
                break;
            case BYTE_ARRAY:
                value = cursor.getBlob(columnIndex);
                break;
            case DOUBLE:
                value = cursor.getDouble(columnIndex);
                break;
            case FLOAT:
                value = cursor.getFloat(columnIndex);
                break;
            case SHORT:
            case BYTE:
                value = cursor.getShort(columnIndex);
                break;
            case BOOLEAN:
                value = cursor.getInt(columnIndex) == 1;
                break;
            case DATE:
                switch (columnMeta.getDateColumnType()) {
                    case TEXT:
                        String date = cursor.getString(columnIndex);
                        if (date != null && date.length() > 0)
                            try {
                                value = sdfDateValue.parse(date);
                            } catch (ParseException e) {
                                throw new LiteDatabaseException("Could not parse date value from database",e);
                            }
                        break;
                    case INTEGER:
                        long unixDate = cursor.getLong(columnIndex);
                        if (unixDate != 0l)
                            value = new Date(unixDate);
                        break;
                }
                break;
            case SERIALIZABLE:
                byte[] data = cursor.getBlob(columnIndex);
                if (data != null && data.length > 0) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = null;
                    try {
                        in = new ObjectInputStream(bis);
                        value = in.readObject();
                    } catch (ClassNotFoundException | IOException e) {
                        throw new LiteDatabaseException("Could not read serializable object from database",e);
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException ex) {
                        }
                        try {
                            if (in != null) {
                                in.close();
                            }
                        } catch (IOException ex) {
                        }
                    }
                }
                break;
        }
        return value;
    }

    /**
     * Parse data from Cursor to object
     * @param object
     * @param cursor
     * @throws LiteDatabaseException
     */
    public void bindObject(final T object, final Cursor cursor)
            throws LiteDatabaseException {
        bindObject(tableClass, object, cursor);
        Class<?> parent = tableClass.getSuperclass();
        if (parent.isAssignableFrom(tableClass.getAnnotation(LiteTable.class).allowedParent())) {
            bindObject(parent, object, cursor);
        }
    }

    /**
     * Parse data from Cursor to object
     * @param clazz
     * @param object
     * @param cursor
     * @throws LiteDatabaseException
     */
    public void bindObject(final Class clazz, final T object, final Cursor cursor) throws LiteDatabaseException {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true); // for private variables
            if (field.getAnnotation(LiteColumn.class) != null) {
                try {
                    field.set(object, getValueFromCursor(cursor, field));
                } catch (IllegalAccessException e) {
                    throw new LiteDatabaseException("Could not set value to object field from database cursor",e);
                }
            }
        }
    }

    /**
     *
     * @param cursor
     * @return array of object with full data from cursor
     * @throws LiteDatabaseException
     */
    public List<T> toList(final Cursor cursor)
            throws LiteDatabaseException {
        List<T> list = new ArrayList<T>();
        if (cursor.moveToFirst()) {
            do {
                list.add(toObject(cursor));
                cursor.moveToNext();
            } while (!cursor.isAfterLast());
        }
        cursor.close();
        return list;
    }

    /**
     *
     * @param cursor
     * @return Object with full data from cursor
     * @throws LiteDatabaseException
     */
    public T toObject(final Cursor cursor) throws LiteDatabaseException {
        T obj;
        try {
            obj = tableClass.newInstance();
        } catch (InstantiationException e) {
            throw new LiteDatabaseException("Could not create new instance of class " + tableClass.getName(),
                    e);
        } catch (IllegalAccessException e) {
            throw new LiteDatabaseException("Could not create new instance of class " + tableClass.getName(),
                    e);
        }

        bindObject(obj, cursor);
        return obj;
    }

    /**
     * Delete all table data
     * @throws LiteDatabaseException
     */
    public void deleteAll() throws LiteDatabaseException {
        delete(null, null);
    }

    /**
     * Delete record by condition
     * @param whereClause
     * @param whereArgs
     * @throws LiteDatabaseException
     */
    public void delete(String whereClause, String[] whereArgs) throws LiteDatabaseException {
            getDatabase()
                    .delete(getTableName(),
                            getLiteQuery().exchange(whereClause),
                            whereArgs);

    }

    /**
     * Delete by object key
     * @param key
     * @throws LiteDatabaseException
     */
    public void deleteByKey(Object key) throws LiteDatabaseException {
        String primaryColumn = getTableMeta().getPrimaryKey();
        delete("[" +
                        (databaseHelper.isUseClassSchema()
                                ? primaryColumn
                                : getTableMeta().getColumns().get(primaryColumn).getColumnName())
                        + "] = ?",
                new String[]{
                        key.toString()
                });
    }

    /**
     * Delete by object
     * @param obj
     * @throws LiteDatabaseException
     */
    public void delete(T obj) throws LiteDatabaseException {
        try {
            deleteByKey(getTableMeta().getColumns().get(getTableMeta().getPrimaryKey()).getValue(obj));
        } catch (IllegalAccessException e) {
            throw new LiteDatabaseException("Could not delete object", e);
        }
    }

    /**
     * Insert new record
     * Not use transaction by default
     * @param obj
     * @return the row id of created object
     * @throws LiteDatabaseException
     */
    public long insert(T obj) throws LiteDatabaseException {
        return insert(obj, false);
    }

    public long insert(T obj, boolean useTransaction) throws LiteDatabaseException {
        final BulkInsert<T> bulkInsert = newBulkInsert(useTransaction);
        bulkInsert.begin();
        try {
            long id = bulkInsert.execute(obj);
            bulkInsert.success();
            return id;
        } finally {
            bulkInsert.end();
        }
    }

    /**
     * Use transaction to insert bulk array of object
     * @param list
     * @return the list of created object id
     * @throws LiteDatabaseException
     */
    public long[] insert(final Collection<T> list) throws LiteDatabaseException {
        return insert(list, true);
    }
    /**
     * Insert bulk array of object
     * @param list
     * @param useTransaction
     * @return the list of created object id
     * @throws LiteDatabaseException
     */
    public long[] insert(final Collection<T> list, boolean useTransaction) throws LiteDatabaseException {
        long[] ids = new long[list.size()];
        final BulkInsert<T> bulkInsert = newBulkInsert(useTransaction);
        bulkInsert.begin();
        try {
            bulkInsert.execute(list);
            bulkInsert.success();
            return ids;
        } finally {
            bulkInsert.end();
        }
    }


    /**
     * Update record by primary key
     * Not use transaction by default
     * @param obj
     * @return the number of rows affected
     * @throws LiteDatabaseException
     */
    public long update(T obj) throws LiteDatabaseException {
        return update(obj, false);
    }

    /**
     * Update record by primary key
     * @param obj
     * @param useTransaction
     * @return
     * @throws LiteDatabaseException
     */
    public long update(T obj, boolean useTransaction) throws LiteDatabaseException {
        final BulkUpdate<T> bulkUpdate = newBulkUpdate(useTransaction);
        bulkUpdate.begin();
        try {
            long length = bulkUpdate.execute(obj);
            bulkUpdate.success();
            return length;
        } finally {
            bulkUpdate.end();
        }
    }

    /**
     *
     * @param list
     * @param useTransaction
     * @return
     * @throws LiteDatabaseException
     */
    public long[] update(Collection<T> list, boolean useTransaction) throws LiteDatabaseException {
        final BulkUpdate<T> bulkUpdate = newBulkUpdate(useTransaction);
        bulkUpdate.begin();
        try {
            long[] data = bulkUpdate.execute(list);
            bulkUpdate.success();
            return data;
        } finally {
            bulkUpdate.end();
        }
    }

    /**
     *
     * @param list
     * @return
     * @throws LiteDatabaseException
     */
    public long[] update(Collection<T> list) throws LiteDatabaseException {
        return update(list, true);
    }

    /**
     * Update table record
     * @param contentValues
     * @param whereClause
     * @param whereArgs
     * @return
     * @throws LiteDatabaseException
     */
    public int update(ContentValues contentValues, String whereClause, String[] whereArgs) throws LiteDatabaseException {
            return getDatabase().update(getTableName(),
                    contentValues,
                    getLiteQuery().exchange(whereClause),
                    whereArgs);
    }

    /**
     * Simple query that call SQLite database query
     * @param distinct
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param orderBy
     * @param limit
     * @return database cursor
     * @throws LiteDatabaseException
     */
    public Cursor query(boolean distinct,
                        String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) throws LiteDatabaseException {
        String sql = SQLiteQueryBuilder.buildQueryString(distinct,
                "[" + (databaseHelper.isUseClassSchema() ? tableClass.getName() : getTableMeta().getTableName()) + "]",
                databaseHelper.isUseClassSchema() ? getTableMeta().getSelectFields() : getColumns(),
                selection,
                groupBy,
                having,
                orderBy,
                limit);
        Log.d(this.getClass().getName(), "Execute query: " + sql);
        return getDatabase().rawQueryWithFactory(null, getLiteQuery().exchange(sql), selectionArgs, null);
    }

    /**
     *
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param orderBy
     * @param limit
     * @return database cursor
     * @throws LiteDatabaseException
     */
    public Cursor query(String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) throws LiteDatabaseException {
        return query(false,
                selection,
                selectionArgs,
                groupBy,
                having,
                orderBy,
                limit);
    }

    /**
     *
     * @param selection
     * @param selectionArgs
     * @param groupBy
     * @param having
     * @param orderBy
     * @return
     * @throws LiteDatabaseException
     */
    public Cursor query(String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy) throws LiteDatabaseException {
        return query(false,
                selection,
                selectionArgs,
                groupBy,
                having,
                orderBy,
                null);
    }

    /**
     *
     * @param selection
     * @param selectionArgs
     * @return database cursor by selection
     * @throws LiteDatabaseException
     */
    public Cursor query(String selection, String[] selectionArgs) throws LiteDatabaseException {
        return query(selection, selectionArgs, null, null, null, null);
    }

    /**
     * Simple raw query
     * @param sql
     * @param args
     * @return raw database cursor
     */
    public Cursor rawQuery(String sql, String[] args) throws LiteDatabaseException {
        return getDatabase().rawQuery(getLiteQuery().exchange(sql), args);
    }

    /**
     * Get object by key
     * @param key
     * @return object contains this primary key value
     * @throws LiteDatabaseException
     */
    public T get(Object key) throws LiteDatabaseException {
        final String primaryColumn = getTableMeta().getPrimaryKey();
        Cursor cursor = query("[" +
                (databaseHelper.isUseClassSchema() ? primaryColumn : getTableMeta().getColumns().get(primaryColumn).getColumnName())
                + "] = ?", new String[] {key.toString()});
        if (cursor.moveToFirst()) {
            try {
                return toObject(cursor);
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * List all records from table
     * @return array of all object on this table
     * @throws LiteDatabaseException
     */
    public List<T> listAll() throws LiteDatabaseException {
        return toList(query(null, null));
    }

    /**
     * Get count of all record
     * @return number of all records
     * @throws LiteDatabaseException
     */
    public int count() throws LiteDatabaseException {
        return count(null, null);
    }

    /**
     * Get count of record by selection
     * @param selection
     * @param selectionArgs
     * @return number of records by selection
     * @throws LiteDatabaseException
     */
    public int count(String selection,  String[] selectionArgs) throws LiteDatabaseException {
        Cursor cursor = null;
        try {
            String sql = "select count(*) from [" +
                    (databaseHelper.isUseClassSchema() ? tableClass.getName() : getTableMeta().getTableName())
                    + "]"
                    + (selection == null ? "" : (" where " + selection));
            cursor = rawQuery(sql,
                    selectionArgs);
            cursor.moveToFirst();
            return cursor.getInt(0);
        }finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     * Get new instance of bulk insert object. To speed up the insertion
     * Use transaction by default
     * @return bulk insert object
     * @throws LiteDatabaseException
     */
    public BulkInsert<T> newBulkInsert() throws LiteDatabaseException {
        return newBulkInsert(true);
    }

    /**
     * Get new instance of bulk insert object. To speed up the insertion
     * @param useTransaction
     * @return
     * @throws LiteDatabaseException
     */
    public BulkInsert<T> newBulkInsert(boolean useTransaction) throws LiteDatabaseException {
        return new BulkInsert<T>(getDatabase(), getDatabaseHelper().getTableMeta(tableClass), useTransaction);
    }

    /**
     * Get new instance of bulk update object. To speed up the updating
     * Use transaction by default
     * @return bulk update object
     * @throws LiteDatabaseException
     */
    public BulkUpdate<T> newBulkUpdate() throws LiteDatabaseException {
        return newBulkUpdate(true);
    }

    /**
     * Get new instance of bulk update object. To speed up the updating
     * @param useTransaction
     * @return
     * @throws LiteDatabaseException
     */
    public BulkUpdate<T> newBulkUpdate(boolean useTransaction) throws LiteDatabaseException {
        return new BulkUpdate<T>(getDatabase(), getDatabaseHelper().getTableMeta(tableClass), useTransaction);
    }

    /**
     * Get table meta data object
     * @return table meta data
     * @throws LiteDatabaseException
     */
    public LiteTableMeta getTableMeta() throws LiteDatabaseException {
        return getDatabaseHelper().getTableMeta(tableClass);
    }

    /**
     * Get table name
     * @return table name
     * @throws LiteDatabaseException
     */
    public String getTableName() throws LiteDatabaseException {
        return getTableMeta().getTableName();
    }

    /**
     * Get columns to select data
     * @return columns of selection data
     * @throws LiteDatabaseException
     */
    public String[] getColumns() throws LiteDatabaseException {
        return getTableMeta().getSelectColumns();
    }
}
