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

package com.luhonghai.litedb.meta;

import com.luhonghai.litedb.LiteColumnType;
import com.luhonghai.litedb.LiteFieldType;

import java.lang.reflect.Field;

/**
 * Created by luhonghai on 9/10/15.
 */
public class LiteColumnMeta {
    /**
     * Save reflect field object
     */
    private Field field;
    /**
     * Column name
     */
    private String columnName;
    /**
     *
     */
    private String alias;
    /**
     * Column type
     */
    private LiteColumnType columnType;
    /**
     * Field type
     */
    private LiteFieldType fieldType;
    /**
     * Date column type
     */
    private LiteColumnType dateColumnType;

    /**
     * Field is primary key
     */
    private boolean isPrimaryKey;

    /**
     * Autoincrement. Support only number field
     */
    private boolean isAutoincrement;

    /**
     * Not allow null
     */
    private boolean isNotNull;
    /**
     * Default value
     */
    private String defaultValue;

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public LiteColumnType getColumnType() {
        return columnType;
    }

    public void setColumnType(LiteColumnType columnType) {
        this.columnType = columnType;
    }

    public LiteFieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(LiteFieldType fieldType) {
        this.fieldType = fieldType;
    }

    public LiteColumnType getDateColumnType() {
        return dateColumnType;
    }

    public void setDateColumnType(LiteColumnType dateColumnType) {
        this.dateColumnType = dateColumnType;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setIsPrimaryKey(boolean isPrimaryKey) {
        this.isPrimaryKey = isPrimaryKey;
    }

    public boolean isAutoincrement() {
        return isAutoincrement;
    }

    public void setIsAutoincrement(boolean isAutoincrement) {
        this.isAutoincrement = isAutoincrement;
    }

    public boolean isNotNull() {
        return isNotNull;
    }

    public void setIsNotNull(boolean isNotNull) {
        this.isNotNull = isNotNull;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object getValue(Object object) throws IllegalAccessException {
        return field.get(object);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
}
