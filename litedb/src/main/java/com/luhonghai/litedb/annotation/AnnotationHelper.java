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
import com.luhonghai.litedb.LiteFieldType;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;
import com.luhonghai.litedb.exception.UnsupportedFieldType;
import com.luhonghai.litedb.meta.LiteColumnMeta;
import com.luhonghai.litedb.meta.LiteTableMeta;

import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by luhonghai on 07/09/15.
 * Help to analyze Lite DB annotation
 */
public class AnnotationHelper {

    private final Class<?> clazz;

    public AnnotationHelper(Class tableClazz) {
        this.clazz = tableClazz;
    }

    /**
     * Get table class
     * @return Table class
     */
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
        if (parent.isAssignableFrom(clazz.getAnnotation(LiteTable.class).allowedParent())) {
            findColumns(columnsList, parent);
        }
        String[] columnsArray = new String[columnsList.size()];
        return columnsList.toArray(columnsArray);
    }

    /**
     * Find columns
     * @param columnsList
     * @param clazz
     */
    private void findColumns(final List<String> columnsList, Class clazz) {
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
        sql.append("[").append(getTableName()).append("]");
        sql.append(" (");
        findColumn(sql, clazz);
        Class<?> parent = clazz.getSuperclass();
        if (parent.isAssignableFrom(clazz.getAnnotation(LiteTable.class).allowedParent())) {
            findColumn(sql, parent);
        }
        String rSql = sql.toString().trim();
        rSql = rSql.substring(0, rSql.length() - 1); // Remove char ,
       return rSql + ");";
    }

    /**
     * Find column is defined in class
     * @param sql
     * @param clazz
     * @throws UnsupportedFieldType
     * @throws InvalidAnnotationData
     */
    private void findColumn(final StringBuffer sql, Class clazz) throws UnsupportedFieldType, InvalidAnnotationData {
        for (Field field : clazz.getDeclaredFields()) {
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            if (liteColumn != null) {
                String fieldType = getColumnType(field);
                sql.append("[").append(getColumnName(field)).append("]");
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
     * @return true if field type is number
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
        sql.append("[").append(getTableName()).append("]");
        sql.append(" ADD COLUMN ");
        sql.append("[").append(getColumnName(field)).append("]");
        sql.append(" ").append(getColumnType(field));
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
     * @param field
     * @return SQLite column type that is matched with field type
     */
    public final String getColumnType(Field field) throws UnsupportedFieldType {
        return getLiteColumnType(field).toString();
    }

    /**
     * Get SQLite type
     * @param field
     * @return SQLite type
     * @throws UnsupportedFieldType
     */
    public final LiteColumnType getLiteColumnType(Field field) throws UnsupportedFieldType {
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
                ) {
            return LiteColumnType.INTEGER;
        } else if (fieldType.isAssignableFrom(String.class)) {
            return  LiteColumnType.TEXT;
        }  else if (fieldType.isAssignableFrom(Byte[].class)
                || fieldType.isAssignableFrom(byte[].class)
                || Serializable.class.isAssignableFrom(fieldType.getClass())
                || Externalizable.class.isAssignableFrom(fieldType.getClass())) {
            return  LiteColumnType.BLOB;
        } else if (fieldType.isAssignableFrom(Double.class)
                || fieldType.isAssignableFrom(double.class)
                || fieldType.isAssignableFrom(Float.class)
                || fieldType.isAssignableFrom(float.class)
                ) {
            return LiteColumnType.REAL;
        } else if (fieldType.isAssignableFrom(Date.class)
                && (liteColumn.dateColumnType() == LiteColumnType.INTEGER
                || liteColumn.dateColumnType() == LiteColumnType.REAL
                || liteColumn.dateColumnType() == LiteColumnType.TEXT)) {
            return LiteColumnType.DATE;
        } else {
            throw new UnsupportedFieldType(clazz, field);
        }
    }

    /**
     * Get field type enum
     * @param field
     * @return field type
     * @throws UnsupportedFieldType
     */
    public final LiteFieldType getLiteFieldType(Field field) throws UnsupportedFieldType {
        Class<?> fieldType = field.getType();
        if (fieldType.isAssignableFrom(Long.class)
                || fieldType.isAssignableFrom(long.class)) {
            return LiteFieldType.LONG;
        } else if (fieldType.isAssignableFrom(Integer.class)
                || fieldType.isAssignableFrom(int.class)) {
            return LiteFieldType.INTEGER;
        } else if (fieldType.isAssignableFrom(Short.class)
                || fieldType.isAssignableFrom(short.class)){
            return LiteFieldType.SHORT;
        } else if (fieldType.isAssignableFrom(Byte.class)
                || fieldType.isAssignableFrom(byte.class)) {
            return LiteFieldType.BYTE;
        } else if (fieldType.isAssignableFrom(Boolean.class)
                || fieldType.isAssignableFrom(boolean.class)
                ) {
            return LiteFieldType.BOOLEAN;
        } else if (fieldType.isAssignableFrom(String.class)) {
            return LiteFieldType.STRING;
        }  else if (fieldType.isAssignableFrom(Byte[].class)
                || fieldType.isAssignableFrom(byte[].class)) {
            return LiteFieldType.BYTE_ARRAY;
        } else if (Serializable.class.isAssignableFrom(fieldType.getClass())
                || Externalizable.class.isAssignableFrom(fieldType.getClass())) {
            return  LiteFieldType.SERIALIZABLE;
        } else if (fieldType.isAssignableFrom(Double.class)
                || fieldType.isAssignableFrom(double.class)) {
            return LiteFieldType.DOUBLE;
        } else if (fieldType.isAssignableFrom(Float.class)
                || fieldType.isAssignableFrom(float.class)
                ) {
            return LiteFieldType.FLOAT;
        } else if (fieldType.isAssignableFrom(Date.class)
                ) {
            return LiteFieldType.DATE;
        } else {
            throw new UnsupportedFieldType(clazz, field);
        }
    }

    /**
     * Get primary field
     * @return primary field
     * @throws InvalidAnnotationData
     */
    public final Field getPrimaryField() throws InvalidAnnotationData {
        return getPrimaryField(clazz);
    }

    /**
     * Get primary field of classes
     * @param targetClass
     * @return Primary field
     * @throws InvalidAnnotationData
     */
    public final Field getPrimaryField(Class<?> targetClass) throws InvalidAnnotationData {
        for (Field field : targetClass.getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true); // for private variables
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            if (liteColumn != null && liteColumn.isPrimaryKey()) {
                return field;
            }
        }
        Class<?> parent = targetClass.getSuperclass();
        if (parent.isAssignableFrom(targetClass.getAnnotation(LiteTable.class).allowedParent())) {
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
     * Generate table meta data of class
     * @return Table meta data
     * @throws AnnotationNotFound
     * @throws InvalidAnnotationData
     * @throws UnsupportedFieldType
     */

    public LiteTableMeta generateTableMeta() throws AnnotationNotFound, InvalidAnnotationData, UnsupportedFieldType {
        LiteTableMeta meta = new LiteTableMeta();
        meta.setTableName(getTableName());
        HashMap<String, LiteColumnMeta> columns = new HashMap<String, LiteColumnMeta>();
        generateTableMeta(meta, columns, clazz);
        Class<?> parent = clazz.getSuperclass();
        if (parent.isAssignableFrom(clazz.getAnnotation(LiteTable.class).allowedParent())) {
            generateTableMeta(meta,columns, parent);
        }
        if (meta.getPrimaryKey().length() == 0)
            throw new InvalidAnnotationData("Require one primary key. Simply to extends LiteEntity class");
        meta.setColumns(columns);
        String[] selectColumns = new String[columns.size()];
        String[] selectFields = new String[columns.size()];
        Iterator<String> fieldNames = columns.keySet().iterator();
        int count = 0;
        List<String> insertFields = new ArrayList<String>();
        List<String> updateFields = new ArrayList<String>();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            LiteColumnMeta columnMeta = columns.get(fieldName);
            selectColumns[count] = "[" + columnMeta.getColumnName() + "]" + ("".equals(columnMeta.getAlias()) ? "" : " AS [" + columnMeta.getAlias() + "]" );
            selectFields[count] = fieldName;
            if (!columnMeta.isAutoincrement() && !insertFields.contains(fieldName)) {
                insertFields.add(fieldName);
            }
            if (!columnMeta.isPrimaryKey() && !columnMeta.isAutoincrement()
                    && !updateFields.contains(fieldName)){
                updateFields.add(fieldName);
            }
            count++;
        }
        meta.setSelectColumns(selectColumns);
        meta.setSelectFields(selectFields);
        String[] listInsertFields = new String[insertFields.size()];
        insertFields.toArray(listInsertFields);
        meta.setInsertFields(listInsertFields);
        String[] listUpdateFields = new String[updateFields.size()];
        updateFields.toArray(listUpdateFields);
        meta.setUpdateFields(listUpdateFields);
        meta.setInsertQuery(generateInsertQuery(meta));
        meta.setUpdateQuery(generateUpdateQuery(meta));
        return meta;
    }

    /**
     * Generate default update query, use for bulk update
     * @param tableMeta
     * @return
     */
    public String generateUpdateQuery(final LiteTableMeta tableMeta) {
        final String[] updateFields = tableMeta.getUpdateFields();
        StringBuffer query = new StringBuffer("UPDATE [" + tableMeta.getTableName() + "] SET ");
        for (int i = 0; i < updateFields.length; i++) {
            query.append("[")
                    .append(tableMeta.getColumns().get(updateFields[i]).getColumnName())
                    .append("]")
                    .append(" = ?");
            if (i < updateFields.length - 1)
                query.append(",");
        }
        query.append(" WHERE ")
                .append("[")
                .append(tableMeta.getColumns().get(tableMeta.getPrimaryKey()).getColumnName())
                .append("]")
                .append(" = ?");
        return query.toString();
    }

    /**
     * Generate default insert query, use for bulk insert
     * @param tableMeta
     * @return insert query
     */
    public String generateInsertQuery(final LiteTableMeta tableMeta) {
        final String[] insertFields = tableMeta.getInsertFields();
        StringBuffer query = new StringBuffer("INSERT INTO [" + tableMeta.getTableName() + "](");
        StringBuffer params = new StringBuffer();
        for (int i = 0; i < insertFields.length; i++) {
            query.append("[").append(tableMeta.getColumns().get(insertFields[i]).getColumnName()).append("]");
            params.append("?");
            if (i < insertFields.length - 1) {
                query.append(",");
                params.append(",");
            }
        }
        query.append(") VALUES (").append(params.toString()).append(")");
        return query.toString();
    }

    /**
     * Find meta data from class
     * @param tableMeta
     * @param columns
     * @param clazz
     * @throws UnsupportedFieldType
     */
    private void generateTableMeta(final LiteTableMeta tableMeta,
                                   final HashMap<String, LiteColumnMeta> columns,
                                   final Class<?> clazz) throws UnsupportedFieldType {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true); // for private variables
            LiteColumn liteColumn = field.getAnnotation(LiteColumn.class);
            if (liteColumn != null) {
                if (liteColumn.isPrimaryKey() && tableMeta.getPrimaryKey().length() == 0)
                    tableMeta.setPrimaryKey(field.getName());
                LiteColumnMeta columnMeta = new LiteColumnMeta();
                columnMeta.setField(field);
                columnMeta.setDateColumnType(liteColumn.dateColumnType());
                String name = liteColumn.name();
                if (name == null || name.length() == 0)
                    name = field.getName();
                columnMeta.setColumnName(name);
                columnMeta.setColumnType(getLiteColumnType(field));
                columnMeta.setFieldType(getLiteFieldType(field));
                columnMeta.setIsAutoincrement(liteColumn.isAutoincrement());
                columnMeta.setIsPrimaryKey(liteColumn.isPrimaryKey());
                columnMeta.setIsNotNull(liteColumn.isNotNull());
                columnMeta.setDefaultValue(liteColumn.defaultValue());
                columnMeta.setAlias(liteColumn.alias());
                columns.put(field.getName(), columnMeta);
            }
        }
    }
}

