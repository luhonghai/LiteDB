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

package com.luhonghai.litedb.ext;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.luhonghai.litedb.LiteBaseDao;
import com.luhonghai.litedb.exception.InvalidAnnotationData;
import com.luhonghai.litedb.exception.LiteDatabaseException;
import com.luhonghai.litedb.meta.LiteColumnMeta;
import com.luhonghai.litedb.meta.LiteTableMeta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by luhonghai on 9/10/15.
 *
 * This class help to speed up the insertion
 *
 * See more http://stackoverflow.com/questions/3501516/android-sqlite-database-slow-insertion
 *
 */
public class BulkInsert {

    private final SQLiteDatabase database;

    private SQLiteStatement sqLiteStatement;

    private final LiteTableMeta tableMeta;

    private final SimpleDateFormat sdfDateValue;

    public BulkInsert(SQLiteDatabase database, LiteTableMeta tableMeta) {
        this.database = database;
        this.tableMeta = tableMeta;
        this.sdfDateValue = new SimpleDateFormat(LiteBaseDao.DEFAULT_DATE_FORMAT, Locale.getDefault());
        this.sdfDateValue.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void begin() {
        database.beginTransaction();
        sqLiteStatement = database.compileStatement(tableMeta.getInsertQuery());
    }

    public long insert(Object object) throws LiteDatabaseException {
        final String[] fields = tableMeta.getInsertFields();
        for (int i = 0; i < fields.length; i++) {
            int index = i + 1;
            final String fieldName = fields[i];
            final LiteColumnMeta meta = tableMeta.getColumns().get(fieldName);
            final Field field = meta.getField();
            field.setAccessible(true);
            final Object fieldValue;
            try {
                fieldValue = field.get(object);
            } catch (IllegalAccessException e) {
                throw new LiteDatabaseException("could not get field value", e);
            }
            if (fieldValue == null) {
                sqLiteStatement.bindNull(index);
                continue;
            }
            switch (meta.getFieldType()) {
                case BOOLEAN:
                    sqLiteStatement.bindLong(index,
                            (Boolean.parseBoolean(fieldValue.toString()) ? 1 : 0));
                    break;
                case BYTE:
                    sqLiteStatement.bindLong(index,
                            Byte.valueOf(fieldValue.toString()));
                    break;
                case SERIALIZABLE:
                case BYTE_ARRAY:
                    ByteArrayOutputStream outputStream = null;
                    ObjectOutputStream objectOutputStream = null;
                    try {
                        outputStream = new ByteArrayOutputStream();
                        objectOutputStream = new ObjectOutputStream(
                                outputStream);
                        objectOutputStream.writeObject(fieldValue);
                        sqLiteStatement.bindBlob(index, outputStream.toByteArray());
                        objectOutputStream.flush();
                        outputStream.flush();
                    } catch (IOException e) {
                        throw new LiteDatabaseException("could not parse byte array data",e);
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
                case DATE:
                    switch (meta.getDateColumnType()) {
                        case TEXT:
                            sqLiteStatement.bindString(index, sdfDateValue.format((Date) fieldValue));
                            break;
                        case REAL:
                            sqLiteStatement.bindDouble(index, new jodd.datetime.JDateTime(((Date) fieldValue).getTime()).getJulianDateDouble());
                            break;
                        case INTEGER:
                            sqLiteStatement.bindLong(index, ((Date) fieldValue).getTime());
                            break;
                        default:
                            throw new LiteDatabaseException("Invalid date column type " + meta.getDateColumnType().toString()
                                , new InvalidAnnotationData("Invalid dateColumnType"));
                    }
                    break;
                case DOUBLE:
                    sqLiteStatement.bindDouble(index, Double.parseDouble(fieldValue.toString()));
                    break;
                case FLOAT:
                    sqLiteStatement.bindDouble(index, Float.parseFloat(fieldValue.toString()));
                    break;
                case INTEGER:
                    sqLiteStatement.bindLong(index, Integer.parseInt(fieldValue.toString()));
                    break;
                case LONG:
                    sqLiteStatement.bindLong(index, Long.parseLong(fieldValue.toString()));
                    break;
                case SHORT:
                    sqLiteStatement.bindLong(index, Short.parseShort(fieldValue.toString()));
                    break;
                case STRING:
                default:
                    sqLiteStatement.bindString(index, fieldValue.toString());
                    break;
            }
        }
        long rowId = sqLiteStatement.executeInsert();
        sqLiteStatement.clearBindings();
        return rowId;
    }

    public void success() {
        database.setTransactionSuccessful();
    }

    public void end() {
        database.endTransaction();
    }
}
