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

import com.luhonghai.litedb.annotation.AnnotationHelper;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by luhonghai on 08/09/15.
 */
public class LiteQuery {

    private final Class[] tableClasses;

    private final Map<String, Map<String, String>> data
            = new ConcurrentHashMap<String, Map<String, String>>();

    public LiteQuery(Class[] tableClasses) throws InvalidAnnotationData, AnnotationNotFound {
        this.tableClasses = tableClasses;
    }

    /**
     * Init mapping data
     */
    private void init() throws AnnotationNotFound {
        synchronized (data) {
            if (data.size() == 0) {
                for (Class clazz : tableClasses) {
                    AnnotationHelper annotationHelper = new AnnotationHelper(clazz);
                    if (!data.containsKey(clazz.getName())) {
                        Map<String, String> items = new ConcurrentHashMap<String, String>();
                        items.put(clazz.getName(), annotationHelper.getTableName());
                        for (Field field : clazz.getDeclaredFields()) {
                            items.put(field.getName(), annotationHelper.getColumnName(field));
                        }
                        data.put(clazz.getName(), items);
                    }
                }
            }
        }
    }
    /**
     * To exchange raw query to SQLite query.
     * Raw query include class name and field name of LiteTable
     * @param sql
     * @return the sql is exchanged to SQLite query
     */
    public String exchange(String sql) throws AnnotationNotFound {
        if (sql == null || sql.length() == 0) return sql;
        for (Class clazz : tableClasses) {
            sql = exchange(clazz, sql);
        }
        return sql;
    }

    /**
     * To exchange raw query to SQLite query.
     * Raw query include class name and field name of LiteTable
     * @param sql
     * @param clazz
     * @return the sql is exchanged by annotation data
     */
    public String exchange(Class clazz, String sql) throws AnnotationNotFound {
        //TODO Must crazy like Datanucleus
        if (sql == null || sql.length() == 0) return sql;
        init();
        Map<String, String> mappingData = data.get(clazz.getName());
        Iterator<String> keys = mappingData.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = mappingData.get(key);
            sql = exchange(sql, key, value);
        }
        return sql;
    }

    /**
     * To exchange raw query to SQLite query.
     * @param sql
     * @param key
     * @param value
     * @return the sql is exchanged by key and value
     */
    public String exchange(String sql, String key, String value) {
        while (sql.contains("["  + key + "]")) {
            sql = sql.replace("[" + key + "]", "[" + value + "]");
        }
        return sql;
    }
}
