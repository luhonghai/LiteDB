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

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.halosolutions.litedb.annotation.AnnotationHelper;
import com.halosolutions.litedb.annotation.LiteColumn;
import com.halosolutions.litedb.exception.AnnotationNotFound;
import com.halosolutions.litedb.exception.InvalidAnnotationData;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luhonghai on 07/09/15.
 */
public abstract class LiteBaseDao<T> {

    public enum LiteField {
        INTEGER("INTEGER"),
        REAL("REAL"),
        TEXT("TEXT"),
        BLOB("BLOB");

        private final String name;

        LiteField(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final AnnotationHelper annotationHelper;

    private final LiteBaseDatabase databaseHelper;

    private final Class<T> tableClass;

    public LiteBaseDao(LiteBaseDatabase databaseHelper, Class tableClass) {
        this.annotationHelper = new AnnotationHelper(tableClass);
        this.databaseHelper = databaseHelper;
        this.tableClass = tableClass;
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
    public LiteBaseDatabase getDatabaseHelper() {
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
     * @return
     * @throws IllegalAccessException
     */
    public ContentValues fillContentValues(final T object)
            throws IllegalAccessException {
        ContentValues contentValues = new ContentValues();
        for (Field field : object.getClass().getDeclaredFields()) {
            LiteColumn fieldEntityAnnotation = field.getAnnotation(LiteColumn.class);
            if (fieldEntityAnnotation != null) {
                if (!fieldEntityAnnotation.isAutoincrement()) {
                    putContentValues(contentValues, field, object);
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
        } else if (fieldValue instanceof Byte[] || fieldValue instanceof byte[]) {
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
     * @return
     * @throws IllegalAccessException
     */
    public Object getValueFromCursor(Cursor cursor, Field field)
            throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        Object value = null;
        int columnIndex = cursor.getColumnIndex(annotationHelper.getColumnName(field));
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
            throws NoSuchFieldException, IllegalAccessException {
        for (Field field : tableClass.getDeclaredFields()) {
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
     * @return
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public List<T> toList(final Cursor cursor)
            throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        List<T> list = new ArrayList<T>();
        if (cursor.moveToFirst()) {
            do {
                list.add(toObject(cursor));
                cursor.moveToNext();
            } while (!cursor.isAfterLast());
        }
        return list;
    }

    /**
     *
     * @param cursor
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws NoSuchFieldException
     */
    public T toObject(final Cursor cursor) throws IllegalAccessException,
            InstantiationException,
            NoSuchFieldException {
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
     * Update record by primary key
     * @param obj
     * @return the number of rows affected
     * @throws IllegalAccessException
     * @throws AnnotationNotFound
     */
    public long update(T obj) throws IllegalAccessException, AnnotationNotFound, InvalidAnnotationData {
        String primaryColumn = annotationHelper.getColumnName(annotationHelper.getPrimaryField());
        return databaseHelper.getDatabase().update(annotationHelper.getTableName(),
                fillContentValues(obj),
                primaryColumn + "=?",
                new String[]{
                        annotationHelper.getPrimaryField().get(obj).toString()
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
     * @return
     * @throws AnnotationNotFound
     */
    public Cursor query(boolean distinct,
                        String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) throws AnnotationNotFound {
        return databaseHelper.getDatabase().query(distinct,
                annotationHelper.getTableName(),
                annotationHelper.getColumns(),
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
     * @param limit
     * @return
     * @throws AnnotationNotFound
     */
    public Cursor query(String selection, String[] selectionArgs, String groupBy,
                        String having, String orderBy, String limit) throws AnnotationNotFound {
        return query(false, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    /**
     *
     * @param selection
     * @param selectionArgs
     * @return
     * @throws AnnotationNotFound
     */
    public Cursor query(String selection, String[] selectionArgs) throws AnnotationNotFound {
        return query(selection, selectionArgs, null, null, null, null);
    }

    /**
     *
     * @param key
     * @return
     * @throws AnnotationNotFound
     * @throws InvalidAnnotationData
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public T get(Object key) throws AnnotationNotFound, InvalidAnnotationData,
            IllegalAccessException, NoSuchFieldException, InstantiationException {
        String primaryColumn = annotationHelper.getColumnName(annotationHelper.getPrimaryField());
        Cursor cursor = query(primaryColumn + "=?", new String[] {key.toString()});
        if (cursor.moveToFirst()) {
            return toObject(cursor);
        }
        return null;
    }

    /**
     *
     * @return
     * @throws AnnotationNotFound
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws InstantiationException
     */
    public List<T> listAll() throws AnnotationNotFound,
            IllegalAccessException, NoSuchFieldException, InstantiationException {
        return toList(query(null, null));
    }

    /**
     * Get count of record
     * @return
     * @throws AnnotationNotFound
     */
    public int count() throws AnnotationNotFound {
        Cursor mCount = databaseHelper.getDatabase().rawQuery("select count(*) from " + annotationHelper.getTableName(), null);
        mCount.moveToFirst();
        int count= mCount.getInt(0);
        mCount.close();
        return count;
    }
}
