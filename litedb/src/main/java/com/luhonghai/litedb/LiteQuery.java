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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jodd.util.StringUtil;

/**
 * Created by luhonghai on 08/09/15.
 */
public class LiteQuery {

    private static final String TAG = "LiteQuery";

    private enum ContextType {
        TABLE_NAME,
        TABLE_ALIAS,
        COLUMN_NAME,
        COLUMN_ALIAS,
        UNKNOWN
    }

    private class QueryContext {

        ContextType type;

        String name;

        int fromIndex;

        int toIndex;

        List<QueryContext> siblings = new ArrayList<QueryContext>();

        QueryContext(ContextType type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof QueryContext
                    && !StringUtil.isEmpty(name)) {
                return name.equals(((QueryContext) o).name);
            }
            return super.equals(o);
        }

        @Override
        public String toString() {
            return name;
        }
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
        return analyzeQuery(sql);
    }

    /**
     * Use ANTLR4 SQLite to analyze query
     * Replace table and column with origin
     * @param sql
     * @return raw SQLite query
     */
    private String analyzeQuery(String sql) {
        final StringBuilder query = new StringBuilder(sql);

        final List<QueryContext> queryContexts = new ArrayList<QueryContext>();

        final Map<String, String> tables = new HashMap<String, String>();
        final Map<String, String> columns = new HashMap<String, String>();

        nl.bigo.sqliteparser.SQLiteLexer lexer = new nl.bigo.sqliteparser.SQLiteLexer(
                new org.antlr.v4.runtime.ANTLRInputStream(sql));
        nl.bigo.sqliteparser.SQLiteParser parser = new nl.bigo.sqliteparser.SQLiteParser(
                new org.antlr.v4.runtime.CommonTokenStream(lexer));
        org.antlr.v4.runtime.tree.ParseTree tree = parser.sql_stmt();
        org.antlr.v4.runtime.tree.ParseTreeWalker.DEFAULT.walk(
                new nl.bigo.sqliteparser.SQLiteBaseListener(){
                    @Override
                    public void enterTable_name(@org.antlr.v4.runtime.misc.NotNull
                                                nl.bigo.sqliteparser.SQLiteParser.Table_nameContext ctx) {
                        logRuleContext("enterTable_name", ctx);
                        QueryContext queryContext = new QueryContext(ContextType.TABLE_NAME);
                        fillQueryContext(queryContext, ctx);
                        String alias = "";
                        if (queryContext.siblings.size() > 1) {
                            if (!queryContext.siblings.get(1).name.equalsIgnoreCase(".")) {
                                // Not column
                                alias = queryContext.siblings.get(queryContext.siblings.size() - 2).name;
                            }
                        }
                        if (!tables.containsKey(queryContext.name) || tables.get(queryContext.name).length() == 0) {
                            tables.put(queryContext.name, alias);
                        }
                        queryContexts.add(queryContext);
                        super.enterTable_name(ctx);
                    }

                    @Override
                    public void enterTable_alias(@org.antlr.v4.runtime.misc.NotNull
                                                 nl.bigo.sqliteparser.SQLiteParser.Table_aliasContext ctx) {
                        logRuleContext("enterTable_alias", ctx);
                        QueryContext queryContext = new QueryContext(ContextType.TABLE_ALIAS);
                        fillQueryContext(queryContext, ctx);
                        String tableName = "";
                        if (queryContext.siblings.size() > 1) {
                            tableName = queryContext.siblings.get(0).name;
                        }
                        if (!tables.containsKey(tableName)
                                || tables.get(tableName).length() == 0) {
                            tables.put(tableName, queryContext.name);
                        }
                        queryContexts.add(queryContext);
                        super.enterTable_alias(ctx);
                    }

                    @Override
                    public void enterColumn_name(@org.antlr.v4.runtime.misc.NotNull
                                                 nl.bigo.sqliteparser.SQLiteParser.Column_nameContext ctx) {

                        logRuleContext("enterColumn_name", ctx);
                        QueryContext queryContext = new QueryContext(ContextType.COLUMN_NAME);
                        fillQueryContext(queryContext, ctx);
                        queryContexts.add(queryContext);
                        super.enterColumn_name(ctx);
                    }

                    @Override
                    public void enterColumn_alias(@org.antlr.v4.runtime.misc.NotNull
                                                  nl.bigo.sqliteparser.SQLiteParser.Column_aliasContext ctx) {
                        logRuleContext("enterColumn_alias", ctx);
                        QueryContext queryContext = new QueryContext(ContextType.COLUMN_ALIAS);
                        fillQueryContext(queryContext, ctx);
                        queryContexts.add(queryContext);
                        super.enterColumn_alias(ctx);
                    }
                }, tree);

        return query.toString();
    }

    /**
     * To exchange raw query to SQLite query.
     * @param sql
     * @param key
     * @param value
     * @return the sql is exchanged by key and value
     */
    @Deprecated
    public String exchange(String sql, String key, String value) {
        while (sql.contains("["  + key + "]")) {
            sql = sql.replace("[" + key + "]", "[" + value + "]");
        }
        return sql;
    }

    private void logRuleContext(String method, org.antlr.v4.runtime.ParserRuleContext ctx) {
        Log.d(TAG, "==================================");
        Log.d(TAG, method + "() called with: " + "ctx = [" + ctx.getText() + "]" + "#" + ctx.getStart().getStartIndex() + "-" + ctx.getStop().getStopIndex()
                + " | parent = [" + ctx.getParent().getText() + "]");
        for (int i = 0; i < ctx.getParent().getChildCount(); i++) {
            Log.d(TAG, method + "() child #" + i + " = " + ctx.getParent().getChild(i).getText());
        }
    }

    private void fillQueryContext(final QueryContext queryContext, final org.antlr.v4.runtime.ParserRuleContext ctx) {
        queryContext.name = ctx.getText();
        queryContext.fromIndex = ctx.getStart().getStartIndex();
        queryContext.toIndex = ctx.getStop().getStopIndex();
        for (int i = 0; i < ctx.getParent().getChildCount(); i++) {
            org.antlr.v4.runtime.tree.ParseTree prc = ctx.getParent().getChild(i);
            QueryContext context = new QueryContext(ContextType.UNKNOWN);
            context.name = prc.getText();
            queryContext.siblings.add(context);
        }
    }
}
