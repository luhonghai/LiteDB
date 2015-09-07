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
package com.halosolutions.litedb.annotation;

import com.halosolutions.litedb.LiteBaseDao;
import com.halosolutions.litedb.exception.AnnotationNotFound;
import com.halosolutions.litedb.exception.InvalidAnnotationData;
import com.halosolutions.litedb.exception.UnsupportedFieldType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by luhonghai on 07/09/15.
 */
public class AnnotationHelper {

    private final Class clazz;

    public AnnotationHelper(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * Get table name
     * @return table name
     * @throws AnnotationNotFound
     */
    public final String getTableName() throws AnnotationNotFound {
        LiteTable annotationTable = (LiteTable) clazz.getAnnotation(LiteTable.class);
        String table = clazz.getSimpleName();
        if (annotationTable != null) {
            if (!annotationTable.name().equals("")) {
                table = annotationTable.name();
            }
        } else {
            throw new AnnotationNotFound(LiteTable.class);
        }
        return table;
    }

    /**
     * Get column name match with field
     * @param field
     * @return Column name
     */
    public final String getColumnName(Field field) {
        LiteColumn annotationColumn = field.getAnnotation(LiteColumn.class);
        String column = null;
        if (annotationColumn != null) {
            if (annotationColumn.name().equals("")) {
                column = field.getName();
            } else {
                column = annotationColumn.name();
            }
        }
        return column;
    }

    /**
     * Get list of columns
     * @return list of columns
     */
    public final String[] getColumns() {
        List<String> columnsList = new ArrayList<String>();
        for (Field field : clazz.getDeclaredFields()) {
            LiteColumn fieldEntityAnnotation = field.getAnnotation(LiteColumn.class);
            if (fieldEntityAnnotation != null) {
                String columnName = getColumnName(field);
                if (columnName != null)
                    columnsList.add(columnName);
            }
        }
        String[] columnsArray = new String[columnsList.size()];
        return columnsList.toArray(columnsArray);
    }

    /**
     * Get query to create table
     * @return query to create table
     * @throws AnnotationNotFound
     */
    public final String getCreateTableQuery() throws AnnotationNotFound, UnsupportedFieldType, InvalidAnnotationData {
        StringBuffer sql = new StringBuffer("CREATE TABLE ");
        sql.append(getTableName());
        sql.append(" (");
        for (Field field : clazz.getDeclaredFields()) {
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            if (liteColumn != null) {
                String fieldType = getFieldType(field);
                sql.append(getColumnName(field));
                sql.append(" ");
                sql.append(fieldType);
                if (liteColumn.isPrimaryKey()) {
                    sql.append(" ").append("PRIMARY KEY");
                }
                if (liteColumn.isAutoincrement()) {
                    verifyAutoincrement(field);
                    sql.append(" ").append("AUTOINCREMENT");
                }
                if (liteColumn.isNotNull()) {
                    sql.append(" ").append("NOT NULL");
                }
                if (liteColumn.defaultValue().length() > 0) {
                    sql.append(" ").append("DEFAULT ").append(liteColumn.defaultValue());
                }
                sql.append(", ");
            }
        }
        String rSql = sql.toString().trim();
        rSql = rSql.substring(0, rSql.length() - 1); // Remove char ,
       return rSql + ");";
    }

    /**
     * Verify if field type is number for autoincrement
     * @param field
     * @throws InvalidAnnotationData
     */
    public void verifyAutoincrement(Field field) throws InvalidAnnotationData {
        if (!(isNumberField(field))) {
            throw new InvalidAnnotationData("Autoincrement only support field type: " +
                    "Double, Float, Long, Integer and Short. Field name: " + field.getName()
                    +". Field type: " + field.getType().getName()
                    +". Class: " + clazz.getName());
        }
    }

    /**
     * Check if field is number
     * @param field
     * @return
     */
    public boolean isNumberField(Field field) {
        Class<?> fType = field.getType();
        return fType.isAssignableFrom(Long.class)
                || fType.isAssignableFrom(long.class)
                || fType.isAssignableFrom(Integer.class)
                || fType.isAssignableFrom(int.class)
                || fType.isAssignableFrom(Short.class)
                || fType.isAssignableFrom(short.class)
                || fType.isAssignableFrom(Double.class)
                || fType.isAssignableFrom(double.class)
                || fType.isAssignableFrom(Float.class)
                || fType.isAssignableFrom(float.class);
    }

    /**
     * Get alter table add column query
     * @param field
     * @return query for alter table add column
     * @throws AnnotationNotFound
     * @throws UnsupportedFieldType
     * @throws InvalidAnnotationData
     */
    public final String getAddColumnQuery(Field field) throws AnnotationNotFound, UnsupportedFieldType, InvalidAnnotationData {
        LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
        if (liteColumn == null) throw new AnnotationNotFound(LiteColumn.class);
        StringBuffer sql = new StringBuffer("ALTER TABLE ");
        sql.append(getTableName());
        sql.append(" ADD COLUMN ");
        sql.append(getColumnName(field));
        sql.append(" ").append(getFieldType(field));
        if (liteColumn.isAutoincrement()) {
            verifyAutoincrement(field);
            sql.append(" ").append("AUTOINCREMENT");
        }
        if (liteColumn.isNotNull()) {
            sql.append(" ").append("NOT NULL");
        }
        if (liteColumn.defaultValue().length() > 0) {
            sql.append(" ").append("DEFAULT ").append(liteColumn.defaultValue());
        }
        return sql.toString();
    }

    /**
     * Get SQLite type
     * INTEGER, TEXT
     * @param field
     * @return
     */
    public final String getFieldType(Field field) throws UnsupportedFieldType {
        Class<?> fieldType = field.getType();
        if (fieldType.isAssignableFrom(Long.class)
                || fieldType.isAssignableFrom(long.class)
                || fieldType.isAssignableFrom(Integer.class)
                || fieldType.isAssignableFrom(int.class)
                || fieldType.isAssignableFrom(Short.class)
                || fieldType.isAssignableFrom(short.class)
                || fieldType.isAssignableFrom(Byte.class)
                || fieldType.isAssignableFrom(byte.class)
                || fieldType.isAssignableFrom(Boolean.class)
                || fieldType.isAssignableFrom(boolean.class)
                ) {
            return LiteBaseDao.LiteField.INTEGER.toString();
        } else if (fieldType.isAssignableFrom(String.class)
                ||fieldType.isAssignableFrom(Date.class)) {
            return  LiteBaseDao.LiteField.TEXT.toString();
        }  else if ((fieldType.isAssignableFrom(Byte[].class)
                || fieldType.isAssignableFrom(byte[].class))) {
            return  LiteBaseDao.LiteField.BLOB.toString();
        } else if ((fieldType.isAssignableFrom(Double.class)
                || fieldType.isAssignableFrom(double.class))
                || fieldType.isAssignableFrom(Float.class)
                || fieldType.isAssignableFrom(float.class)) {
            return  LiteBaseDao.LiteField.REAL.toString();
        } else {
            throw new UnsupportedFieldType(clazz, field);
        }
    }

    public final Field getPrimaryField() throws InvalidAnnotationData {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true); // for private variables
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            if (liteColumn != null && liteColumn.isPrimaryKey()) {
                return field;
            }
        }
        throw new InvalidAnnotationData("No primary key found for table " + clazz.getName());
    }
}
