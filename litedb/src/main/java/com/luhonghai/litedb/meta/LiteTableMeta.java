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

import java.util.HashMap;

/**
 * Created by luhonghai on 9/10/15.
 * Table meta data save for quick access
 */
public class LiteTableMeta {
    /**
     * Table name
     */
    private String tableName;
    /**
     * Raw query to insert object
     */
    private String insertQuery;
    /**
     * Raw query to update object
     */
    private String updateQuery;

    /**
     * Primary key name
     */
    private String primaryKey;
    /**
     * Fields to insert to database
     * Not include autoincrement field
     * Same order as raw insert query
     */
    private String[] insertFields;
    /**
     * Fields to update to database
     * Not include autoincrement field and primary key
     * Same order as raw update query
     */
    private String[] updateFields;
    /**
     * Store all column meta data
     */
    private HashMap<String, LiteColumnMeta> columns;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getInsertQuery() {
        return insertQuery;
    }

    public void setInsertQuery(String insertQuery) {
        this.insertQuery = insertQuery;
    }

    public String getUpdateQuery() {
        return updateQuery;
    }

    public void setUpdateQuery(String updateQuery) {
        this.updateQuery = updateQuery;
    }

    public String getPrimaryKey() {
        if (primaryKey == null) return "";
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String[] getInsertFields() {
        return insertFields;
    }

    public void setInsertFields(String[] insertFields) {
        this.insertFields = insertFields;
    }

    public String[] getUpdateFields() {
        return updateFields;
    }

    public void setUpdateFields(String[] updateFields) {
        this.updateFields = updateFields;
    }

    public HashMap<String, LiteColumnMeta> getColumns() {
        return columns;
    }

    public void setColumns(HashMap<String, LiteColumnMeta> columns) {
        this.columns = columns;
    }
}
