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

package com.luhonghai.litedb.bulk;

import android.database.sqlite.SQLiteDatabase;

import com.luhonghai.litedb.exception.LiteDatabaseException;
import com.luhonghai.litedb.meta.LiteTableMeta;

/**
 * Created by luhonghai on 9/10/15.
 *
 * This class help to speed up the insertion
 *
 * See more http://stackoverflow.com/questions/3501516/android-sqlite-database-slow-insertion
 *
 */
public class BulkInsert<T> extends AbstractBulk<T> {

    public BulkInsert(SQLiteDatabase database, LiteTableMeta tableMeta) {
        super(database, tableMeta);
    }

    public BulkInsert(SQLiteDatabase database, LiteTableMeta tableMeta, boolean useTransaction) {
        super(database, tableMeta, useTransaction);
    }

    @Override
    protected String getQuery() {
        return getTableMeta().getInsertQuery();
    }

    @Override
    public long execute(T object) throws LiteDatabaseException {
        final String[] fields = getTableMeta().getInsertFields();
        for (int i = 0; i < fields.length; i++) {
            int index = i + 1;
            final String fieldName = fields[i];
            bindObject(object, fieldName, index);
        }
        long rowId = getSqLiteStatement().executeInsert();
        getSqLiteStatement().clearBindings();
        return rowId;
    }
}
