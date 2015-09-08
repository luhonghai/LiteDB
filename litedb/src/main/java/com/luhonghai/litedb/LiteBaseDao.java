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
import android.util.Log;

import com.luhonghai.litedb.annotation.AnnotationHelper;
import com.luhonghai.litedb.annotation.LiteColumn;
import com.luhonghai.litedb.annotation.LiteTable;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jodd.datetime.JDateTime;

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
     * @throws InvalidAnnotationData
     * @throws AnnotationNotFound
     */
    public LiteBaseDao(LiteDatabaseHelper databaseHelper, Class<T> tableClass)
            throws InvalidAnnotationData, AnnotationNotFound {
        this.annotationHelper = new AnnotationHelper(tableClass);
        this.databaseHelper = databaseHelper;
        this.tableClass = tableClass;
        this.sdfDateValue = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.getDefault());
        this.sdfDateValue.setTimeZone(TimeZone.getTimeZone("UTC"));
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
     * @throws IllegalAccessException
     */
    public ContentValues fillContentValues(final T object)
            throws IllegalAccessException {
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
     * @throws IllegalAccessException
     */
    public void putContentValues(final ContentValues contentValues, final Field field,
                                   final T object) throws IllegalAccessException {
        if (!field.isAccessible())
            field.setAccessible(true); // for private variables
        Object fieldValue = field.get(object);
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
                    contentValues.put(key, new JDateTime(((Date) fieldValue).getTime()).getJulianDateDouble());
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
     * @throws IllegalAccessException
     */
    public Object getValueFromCursor(Cursor cursor, Field field)
            throws IllegalAccessException, ParseException, ClassNotFoundException, IOException {
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
                        value = sdfDateValue.parse(date);
                    break;
                case REAL:
                    double jdDate = cursor.getDouble(columnIndex);
                    if (jdDate != 0.0d)
                        value = new JDateTime(jdDate).convertToDate();
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
                    throw e;
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
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void bindObject(final T object, final Cursor cursor)
            throws NoSuchFieldException, IllegalAccessException, ParseException, IOException, ClassNotFoundException {
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
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ParseException
     */
    public void bindObject(final Class clazz, final T object, final Cursor cursor)
            throws NoSuchFieldException, IllegalAccessException, ParseException, IOException, ClassNotFoundException {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true); // for private variables
            if (field.getAnnotation(LiteColumn.class) != null) {
                field.set(object, getValueFromCursor(cursor, field));
            }
        }
    }

    /**
     *
     * @param cursor
     * @return array of object with full data from cursor
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public List<T> toList(final Cursor cursor)
            throws IllegalAccessException, NoSuchFieldException, InstantiationException, ParseException
            , IOException, ClassNotFoundException {
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
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     */
    public T toObject(final Cursor cursor) throws IllegalAccessException,
            InstantiationException,
            NoSuchFieldException, ParseException, IOException, ClassNotFoundException {
        T obj = tableClass.newInstance();
        bindObject(obj, cursor);
        return obj;
    }

    /**
     * Delete all table data
     * @throws AnnotationNotFound
     */
    public void deleteAll() throws AnnotationNotFound {
        databaseHelper.getDatabase()
                    .delete(annotationHelper.getTableName(), null, null);
    }

    /**
     * Delete by object key
     * @param key
     */
    public void deleteByKey(Object key) throws AnnotationNotFound, InvalidAnnotationData {
        String primaryColumn = annotationHelper.getColumnName(annotationHelper.getPrimaryField());
        databaseHelper.getDatabase()
                .delete(annotationHelper.getTableName(),
                        primaryColumn + "=?",
                        new String[]{
                                key.toString()
                        });
    }

    /**
     * Delete by object
     * @param obj
     * @throws IllegalAccessException
     * @throws AnnotationNotFound
     * @throws InvalidAnnotationData
     */
    public void delete(T obj) throws InvalidAnnotationData, IllegalAccessException, AnnotationNotFound {
        deleteByKey(annotationHelper.getPrimaryField().get(obj));
    }

    /**
     * Insert new record
     * @param obj
     * @return the row id of created object
     * @throws IllegalAccessException
     * @throws AnnotationNotFound
     */
    public long insert(T obj) throws IllegalAccessException, AnnotationNotFound {
        return databaseHelper.getDatabase().insert(
                annotationHelper.getTableName(), null, fillContentValues(obj));
    }

    /**
     * Use transaction to insert bulk array of object
     * @param list
     * @return the list of created object id
     * @throws AnnotationNotFound
     * @throws IllegalAccessException
     */
    public long[] insert(final List<T> list) throws AnnotationNotFound, IllegalAccessException {
        return insert(list, true);
    }
    /**
     * Insert bulk array of object
     * @param list
     * @param useTransaction
     * @return the list of created object id
     * @throws AnnotationNotFound
     * @throws IllegalAccessException
     */
    public long[] insert(final List<T> list, boolean useTransaction) throws AnnotationNotFound, IllegalAccessException {
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
     * @throws IllegalAccessException
     * @throws AnnotationNotFound
     */
    public long update(T obj) throws IllegalAccessException, AnnotationNotFound, InvalidAnnotationData {
        String primaryColumn = annotationHelper.getColumnName(annotationHelper.getPrimaryField());
        String primaryValue = annotationHelper.getPrimaryField().get(obj).toString();
        return databaseHelper.getDatabase().update(annotationHelper.getTableName(),
                fillContentValues(obj),
                "[" + primaryColumn + "] = ?",
                new String[]{
                        primaryValue
                });
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
     * @throws AnnotationNotFound
     */
    public Cursor query(boolean distinct,
                        String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) throws AnnotationNotFound {
        return databaseHelper.getDatabase().query(distinct,
                annotationHelper.getTableName(),
                annotationHelper.getColumns(),
                databaseHelper.getLiteQuery().exchange(tableClass,selection),
                selectionArgs,
                databaseHelper.getLiteQuery().exchange(tableClass,groupBy),
                databaseHelper.getLiteQuery().exchange(tableClass,having),
                databaseHelper.getLiteQuery().exchange(tableClass,orderBy),
                limit);
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
     * @throws AnnotationNotFound
     */
    public Cursor query(String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) throws AnnotationNotFound {
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
     * @return database cursor by selection
     * @throws AnnotationNotFound
     */
    public Cursor query(String selection, String[] selectionArgs) throws AnnotationNotFound {
        return query(selection, selectionArgs, null, null, null, null);
    }

    /**
     * Simple raw query
     * @param sql
     * @param args
     * @return raw database cursor
     */
    public Cursor rawQuery(String sql, String[] args) throws AnnotationNotFound {
        return getDatabaseHelper().getDatabase().rawQuery(databaseHelper.getLiteQuery().exchange(sql), args);
    }

    /**
     * Get object by key
     * @param key
     * @return object contains this primary key value
     * @throws AnnotationNotFound
     * @throws InvalidAnnotationData
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public T get(Object key) throws AnnotationNotFound, InvalidAnnotationData,
            IllegalAccessException, NoSuchFieldException, InstantiationException, ParseException, IOException, ClassNotFoundException {
        String primaryColumn = annotationHelper.getColumnName(annotationHelper.getPrimaryField());
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
     * @throws AnnotationNotFound
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public List<T> listAll() throws AnnotationNotFound,
            IllegalAccessException, NoSuchFieldException, InstantiationException, ParseException, IOException, ClassNotFoundException {
        return toList(query(null, null));
    }

    /**
     * Get count of all record
     * @return number of all records
     * @throws AnnotationNotFound
     */
    public int count() throws AnnotationNotFound {
        return count(null, null);
    }

    /**
     * Get count of record by selection
     * @param selection
     * @param selectionArgs
     * @return number of records by selection
     * @throws AnnotationNotFound
     */
    public int count(String selection,  String[] selectionArgs) throws AnnotationNotFound {
        Cursor cursor = databaseHelper.getDatabase().rawQuery(
                "select count(*) from [" + annotationHelper.getTableName() + "]"
                        + (selection == null ? "" : (" where " + databaseHelper.getLiteQuery().exchange(tableClass, selection))),
                selectionArgs);
        cursor.moveToFirst();
        int count= cursor.getInt(0);
        cursor.close();
        return count;
    }


}
