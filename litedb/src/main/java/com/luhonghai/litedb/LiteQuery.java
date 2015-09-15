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

import android.util.Log;

import com.luhonghai.litedb.exception.AnnotationNotFound;

import org.antlr.v4.runtime.misc.NotNull;

import java.util.Iterator;
import java.util.Map;

import nl.bigo.sqliteparser.SQLiteParser;

/**
 * Created by luhonghai on 08/09/15.
 */
public class LiteQuery {

    private static final String TAG = "LiteQuery";

    interface QueryAnalyzeListener {

        void onFoundTable(String table);

        void onFoundColumn(String table, String column);
    }

    private final LiteDatabaseHelper liteDatabaseHelper;

    public LiteQuery(LiteDatabaseHelper liteDatabaseHelper) {
        this.liteDatabaseHelper = liteDatabaseHelper;
    }
        /**
     * To exchange raw query to SQLite query.
     * Raw query include class name and field name of LiteTable
     * @param sql
     * @return the sql is exchanged to SQLite query
     */
    public String exchange(String sql)  {
        if (sql == null || sql.length() == 0 || !liteDatabaseHelper.isUseClassSchema()) return sql;
        final StringBuffer stringBuffer = new StringBuffer(sql);
        analyzeQuery(sql + ";", new QueryAnalyzeListener() {
            @Override
            public void onFoundTable(String table) {
                Log.d(TAG, "onFoundTable " + table);
            }

            @Override
            public void onFoundColumn(String table, String column) {
                Log.d(TAG, "onFoundColumn " + column);
            }
        });
        return sql;
    }

    private void analyzeQuery(String sql, final QueryAnalyzeListener listener) {
        nl.bigo.sqliteparser.SQLiteLexer lexer = new nl.bigo.sqliteparser.SQLiteLexer(
                new org.antlr.v4.runtime.ANTLRInputStream(sql));
        nl.bigo.sqliteparser.SQLiteParser parser = new nl.bigo.sqliteparser.SQLiteParser(
                new org.antlr.v4.runtime.CommonTokenStream(lexer));
        // Invoke the `select_stmt` production.
        org.antlr.v4.runtime.tree.ParseTree tree = parser.select_stmt();
        org.antlr.v4.runtime.tree.ParseTreeWalker.DEFAULT.walk(
                new nl.bigo.sqliteparser.SQLiteBaseListener(){
            @Override
            public void enterExpr(
                    nl.bigo.sqliteparser.SQLiteParser.ExprContext ctx) {
                if (ctx.table_name() != null) {
                    listener.onFoundTable(ctx.table_name().getText());
                } else if (ctx.column_name() != null) {
                    listener.onFoundColumn("", ctx.column_name().getText());
                }
            }
        }, tree);
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
