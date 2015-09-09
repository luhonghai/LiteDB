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
import android.util.Log;

import com.luhonghai.litedb.annotation.AnnotationHelper;
import com.luhonghai.litedb.annotation.LiteColumn;
import com.luhonghai.litedb.annotation.LiteTable;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;
import com.luhonghai.litedb.exception.LiteDatabaseException;

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
import java.util.Date;
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
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
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
        this.annotationHelper = new AnnotationHelper(tableClass);
        this.databaseHelper = databaseHelper;
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
        for (Field field : object.getClass().getDeclaredFields()) {
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            if (liteColumn != null) {
                if (!liteColumn.isAutoincrement()) {
                        putContentValues(contentValues,
                                field,
                                object);
                }
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
        if (!field.isAccessible())
            field.setAccessible(true); // for private variables
        Object fieldValue = null;
        try {
            fieldValue = field.get(object);
        } catch (IllegalAccessException e) {
            throw new LiteDatabaseException("Could not get field value from object",e);
        }
        if (fieldValue == null) return;
        String key = annotationHelper.getColumnName(field);
        if (fieldValue instanceof Long) {
            contentValues.put(key, Long.valueOf(fieldValue.toString()));
        } else if (fieldValue instanceof String) {
            contentValues.put(key, fieldValue.toString());
        } else if (fieldValue instanceof Integer) {
            contentValues.put(key, Integer.valueOf(fieldValue.toString()));
        } else if (fieldValue instanceof Float) {
            contentValues.put(key, Float.valueOf(fieldValue.toString()));
        } else if (fieldValue instanceof Byte) {
            contentValues.put(key, Byte.valueOf(fieldValue.toString()));
        } else if (fieldValue instanceof Short) {
            contentValues.put(key, Short.valueOf(fieldValue.toString()));
        } else if (fieldValue instanceof Boolean) {
            contentValues.put(key, Boolean.parseBoolean(fieldValue.toString()));
        } else if (fieldValue instanceof Double) {
            contentValues.put(key, Double.valueOf(fieldValue.toString()));
        } else if(fieldValue instanceof Date) {
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            switch (liteColumn.dateColumnType()) {
                case TEXT:
                    contentValues.put(key, sdfDateValue.format((Date) fieldValue));
                    break;
                case REAL:
                    try {
                        Class<?> jdateClass = Class.forName("jodd.datetime.JDateTime");
                        Object obj = jdateClass.getConstructor(long.class).newInstance(((Date) fieldValue).getTime());
                        Method method = jdateClass.getMethod("getJulianDateDouble");
                        contentValues.put(key, (double) method.invoke(obj));
                    } catch (Exception e) {
                        throw new LiteDatabaseException("Could not set date value of REAL type", e);
                    }
                    break;
                case INTEGER:
                    contentValues.put(key, ((Date) fieldValue).getTime());
                    break;
            }
        } else if (fieldValue instanceof Byte[] || fieldValue instanceof byte[]
                || Serializable.class.isAssignableFrom(fieldValue.getClass())
                || Externalizable.class.isAssignableFrom(fieldValue.getClass())) {
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
        }
    }

    /**
     * Get content from specific types
     * @param cursor
     * @param field
     * @return value by special field from cursor
     * @throws LiteDatabaseException
     */
    public Object getValueFromCursor(Cursor cursor, Field field)
            throws LiteDatabaseException {
        Class<?> fieldType = field.getType();
        Object value = null;
        int columnIndex = cursor.getColumnIndex(annotationHelper.getColumnName(field));
        if (columnIndex == -1) return null;
        if (fieldType.isAssignableFrom(Long.class)
                || fieldType.isAssignableFrom(long.class)) {
            value = cursor.getLong(columnIndex);
        } else if (fieldType.isAssignableFrom(String.class)) {
            value = cursor.getString(columnIndex);
        } else if ((fieldType.isAssignableFrom(Integer.class) || fieldType
                .isAssignableFrom(int.class))) {
            value = cursor.getInt(columnIndex);
        } else if ((fieldType.isAssignableFrom(Byte[].class) || fieldType
                .isAssignableFrom(byte[].class))) {
            value = cursor.getBlob(columnIndex);
        } else if ((fieldType.isAssignableFrom(Double.class) || fieldType
                .isAssignableFrom(double.class))) {
            value = cursor.getDouble(columnIndex);
        } else if ((fieldType.isAssignableFrom(Float.class) || fieldType
                .isAssignableFrom(float.class))) {
            value = cursor.getFloat(columnIndex);
        } else if ((fieldType.isAssignableFrom(Short.class) || fieldType
                .isAssignableFrom(short.class))) {
            value = cursor.getShort(columnIndex);
        } else if (fieldType.isAssignableFrom(Byte.class)
                || fieldType.isAssignableFrom(byte.class)) {
            value = (byte) cursor.getShort(columnIndex);
        } else if (fieldType.isAssignableFrom(Boolean.class)
                || fieldType.isAssignableFrom(boolean.class)) {
            int booleanInteger = cursor.getInt(columnIndex);
            value = booleanInteger == 1;
        } else if (fieldType.isAssignableFrom(Date.class)) {
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            switch (liteColumn.dateColumnType()) {
                case TEXT:
                    String date = cursor.getString(columnIndex);
                    if (date != null && date.length() > 0)
                        try {
                            value = sdfDateValue.parse(date);
                        } catch (ParseException e) {
                            throw new LiteDatabaseException("Could not parse date value from database",e);
                        }
                    break;
                case REAL:
                    double jdDate = cursor.getDouble(columnIndex);
                    if (jdDate != 0.0d) {
                        try {
                            Class<?> jdateClass = Class.forName("jodd.datetime.JDateTime");
                            Object obj = jdateClass.getConstructor(double.class).newInstance(jdDate);
                            Method method = jdateClass.getMethod("convertToDate");
                            value = method.invoke(obj);
                        } catch (Exception e) {
                            throw new LiteDatabaseException("Could not get date value from field type REAL", e);
                        }
                    }
                    break;
                case INTEGER:
                    long unixDate = cursor.getLong(columnIndex);
                    if (unixDate != 0l)
                        value = new Date(unixDate);
                    break;
            }
        } else if (Serializable.class.isAssignableFrom(fieldType.getClass())
                || Externalizable.class.isAssignableFrom(fieldType.getClass())) {
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
        try {
            databaseHelper.getDatabase()
                    .delete(annotationHelper.getTableName(),
                            getDatabaseHelper().getLiteQuery().exchange(whereClause),
                            whereArgs);
        } catch (AnnotationNotFound e) {
            throw new LiteDatabaseException("",e);
        }
    }

    /**
     * Delete by object key
     * @param key
     * @throws LiteDatabaseException
     */
    public void deleteByKey(Object key) throws LiteDatabaseException {
        try {
            delete("[" + annotationHelper.getPrimaryField().getName() + "] = ?",
                    new String[]{
                            key.toString()
                    });
        } catch (InvalidAnnotationData ex) {
            throw new LiteDatabaseException("Could not delete object by key " + key, ex);
        }
    }

    /**
     * Delete by object
     * @param obj
     * @throws LiteDatabaseException
     */
    public void delete(T obj) throws LiteDatabaseException {
        try {
            deleteByKey(annotationHelper.getPrimaryField().get(obj));
        } catch (IllegalAccessException | InvalidAnnotationData e) {
            throw new LiteDatabaseException("Could not delete object", e);
        }
    }

    /**
     * Insert new record
     * @param obj
     * @return the row id of created object
     * @throws LiteDatabaseException
     */
    public long insert(T obj) throws LiteDatabaseException {
        try {
            return databaseHelper.getDatabase().insert(
                    annotationHelper.getTableName(), null, fillContentValues(obj));
        } catch (AnnotationNotFound annotationNotFound) {
            throw new LiteDatabaseException("Could not insert new object", annotationNotFound);
        }
    }

    /**
     * Use transaction to insert bulk array of object
     * @param list
     * @return the list of created object id
     * @throws LiteDatabaseException
     */
    public long[] insert(final List<T> list) throws LiteDatabaseException {
        return insert(list, true);
    }
    /**
     * Insert bulk array of object
     * @param list
     * @param useTransaction
     * @return the list of created object id
     * @throws LiteDatabaseException
     */
    public long[] insert(final List<T> list, boolean useTransaction) throws LiteDatabaseException {
        if (useTransaction)
            databaseHelper.getDatabase().beginTransaction();
        try {
            long[] ids = new long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                final T obj = list.get(i);
                ids[i] = insert(obj);
            }
            if (useTransaction)
                databaseHelper.getDatabase().setTransactionSuccessful();
            return ids;
        } finally {
            if (useTransaction && databaseHelper.getDatabase().inTransaction())
                databaseHelper.getDatabase().endTransaction();
        }
    }


    /**
     * Update record by primary key
     * @param obj
     * @return the number of rows affected
     * @throws LiteDatabaseException
     */
    public int update(T obj) throws LiteDatabaseException {
        try {
            String primaryColumn = annotationHelper.getPrimaryField().getName();
            String primaryValue = annotationHelper.getPrimaryField().get(obj).toString();
            return update(
                    fillContentValues(obj),
                    "[" + primaryColumn + "] = ?",
                    new String[]{
                            primaryValue
                    });
        } catch (InvalidAnnotationData | IllegalAccessException e) {
            throw new LiteDatabaseException("Could not update object", e);
        }
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
        try {
            return databaseHelper.getDatabase().update(annotationHelper.getTableName(),
                    contentValues,
                    getDatabaseHelper().getLiteQuery().exchange(whereClause),
                    whereArgs);
        } catch (AnnotationNotFound e) {
            throw new LiteDatabaseException("Could not update record",e);
        }
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
        try {
            return databaseHelper.getDatabase().query(distinct,
                    annotationHelper.getTableName(),
                    annotationHelper.getColumns(),
                    databaseHelper.getLiteQuery().exchange(tableClass, selection),
                    selectionArgs,
                    databaseHelper.getLiteQuery().exchange(tableClass, groupBy),
                    databaseHelper.getLiteQuery().exchange(tableClass, having),
                    databaseHelper.getLiteQuery().exchange(tableClass, orderBy),
                    limit);
        } catch (AnnotationNotFound e) {
            throw new LiteDatabaseException("Could not query table " + tableClass.getName() ,e);
        }
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
        try {
            return getDatabaseHelper().getDatabase().rawQuery(databaseHelper.getLiteQuery().exchange(sql), args);
        } catch (AnnotationNotFound e) {
            throw new LiteDatabaseException("Could not execute raw query",e);
        }
    }

    /**
     * Get object by key
     * @param key
     * @return object contains this primary key value
     * @throws LiteDatabaseException
     */
    public T get(Object key) throws LiteDatabaseException {
        String primaryColumn;
        try {
            primaryColumn = annotationHelper.getPrimaryField().getName();
        } catch (InvalidAnnotationData e) {
            throw new LiteDatabaseException("Could not get primary field",e);
        }
        Cursor cursor = query("[" + primaryColumn + "] = ?", new String[] {key.toString()});
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
        Cursor cursor;
        try {
            cursor = databaseHelper.getDatabase().rawQuery(
                    "select count(*) from [" + annotationHelper.getTableName() + "]"
                            + (selection == null ? "" : (" where " + databaseHelper.getLiteQuery().exchange(tableClass, selection))),
                    selectionArgs);
        } catch (AnnotationNotFound e) {
            throw new LiteDatabaseException("Could not get count of table " + tableClass.getName(),e);
        }
        cursor.moveToFirst();
        int count= cursor.getInt(0);
        cursor.close();
        return count;
    }


}
