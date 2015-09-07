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
package com.luhonghai.litedb.annotation;

import com.luhonghai.litedb.LiteColumnType;
import com.luhonghai.litedb.LiteEntity;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;
import com.luhonghai.litedb.exception.UnsupportedFieldType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by luhonghai on 07/09/15.
 */
public class AnnotationHelper {

    private final Class clazz;

    public AnnotationHelper(Class tableClazz) {
        this.clazz = tableClazz;
    }

    public Class getTableClass() {
        return clazz;
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
        findColumns(columnsList, clazz);
        Class<?> parent = clazz.getSuperclass();
        if (parent.isAssignableFrom(LiteEntity.class)) {
            findColumns(columnsList, parent);
        }
        String[] columnsArray = new String[columnsList.size()];
        return columnsList.toArray(columnsArray);
    }

    private final void findColumns(final List<String> columnsList, Class clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            LiteColumn fieldEntityAnnotation = field.getAnnotation(LiteColumn.class);
            if (fieldEntityAnnotation != null) {
                String columnName = getColumnName(field);
                if (columnName != null && !columnsList.contains(columnName))
                    columnsList.add(columnName);
            }
        }
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
        findColumn(sql, clazz);
        Class<?> parent = clazz.getSuperclass();
        if (parent.isAssignableFrom(LiteEntity.class)) {
            findColumn(sql, parent);
        }
        String rSql = sql.toString().trim();
        rSql = rSql.substring(0, rSql.length() - 1); // Remove char ,
       return rSql + ");";
    }

    private final void findColumn(final StringBuffer sql, Class clazz) throws UnsupportedFieldType, InvalidAnnotationData {
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
        LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
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
                || (liteColumn.dateColumnType() == LiteColumnType.INTEGER
                    && fieldType.isAssignableFrom(Date.class))
                ) {
            return LiteColumnType.INTEGER.toString();
        } else if (fieldType.isAssignableFrom(String.class)
                ||fieldType.isAssignableFrom(Date.class)
                || (liteColumn.dateColumnType() == LiteColumnType.TEXT
                && fieldType.isAssignableFrom(Date.class))) {
            return  LiteColumnType.TEXT.toString();
        }  else if ((fieldType.isAssignableFrom(Byte[].class)
                || fieldType.isAssignableFrom(byte[].class))) {
            return  LiteColumnType.BLOB.toString();
        } else if (fieldType.isAssignableFrom(Double.class)
                || fieldType.isAssignableFrom(double.class)
                || fieldType.isAssignableFrom(Float.class)
                || fieldType.isAssignableFrom(float.class)
                || (liteColumn.dateColumnType() == LiteColumnType.REAL
                && fieldType.isAssignableFrom(Date.class))) {
            return  LiteColumnType.REAL.toString();
        } else {
            throw new UnsupportedFieldType(clazz, field);
        }
    }

    public final Field getPrimaryField() throws InvalidAnnotationData {
        return getPrimaryField(clazz);
    }

    public final Field getPrimaryField(Class targetClass) throws InvalidAnnotationData {
        for (Field field : targetClass.getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true); // for private variables
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            if (liteColumn != null && liteColumn.isPrimaryKey()) {
                return field;
            }
        }
        Class<?> parent = targetClass.getSuperclass();
        if (parent.isAssignableFrom(LiteEntity.class)) {
            for (Field field : parent.getDeclaredFields()) {
                if (!field.isAccessible())
                    field.setAccessible(true); // for private variables
                LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
                if (liteColumn != null && liteColumn.isPrimaryKey()) {
                    return field;
                }
            }
        }
        throw new InvalidAnnotationData("No primary key found for table " + clazz.getName());
    }

    /**
     * To exchange raw query to SQLite query.
     * Raw query include class name and field name of LiteTable
     * @param sql
     * @return
     */
    public String exchange(String sql) {
        //TODO Must crazy like Datanucleus
        return sql;
    }
}
