/**
 * The MIT License (MIT)

 Copyright (C) 2013 bkiers, https://github.com/bkiers

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.luhonghai.litedb;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.util.Log;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

import nl.bigo.sqliteparser.SQLiteBaseListener;
import nl.bigo.sqliteparser.SQLiteLexer;
import nl.bigo.sqliteparser.SQLiteParser;

/**
 * Created by luhonghai on 15/09/15.
 */
public class SQLiteParserTest extends ApplicationTestCase<Application> {

    private static final String TAG = "TestSQLiteParser";

    public SQLiteParserTest() {
        super(Application.class);
    }

    public void testSQLiteParser() {
        // The list that will hold our function names.
        final List<String> functionNames = new ArrayList<String>();

        // The select-statement to be parsed.
        String sql = "select [account name] as name, g.name as [group name] from account A inner join group as G on G.[account id] = [A].[id] where A.id=?";
        //String sql = "update account set id=? where ac=?";

        // Create a lexer and parser for the input.
        SQLiteLexer lexer = new SQLiteLexer(new ANTLRInputStream(sql));
        SQLiteParser parser = new SQLiteParser(new CommonTokenStream(lexer));

        // Invoke the `select_stmt` production.
        ParseTree tree = parser.sql_stmt();

        // Walk the `select_stmt` production and listen when the parser
        // enters the `expr` production.
        ParseTreeWalker.DEFAULT.walk(new SQLiteBaseListener() {

            @Override
            public void enterTable_name(@NotNull SQLiteParser.Table_nameContext ctx) {
                Log.d(TAG, "==================================");
                Log.d(TAG, "enterTable_name() called with: " + "ctx = [" + ctx.getText() + "]" + "#" + ctx.getStart().getStartIndex() + "-" + ctx.getStop().getStopIndex()
                        + " | parent = [" + ctx.getParent().getText() + "]");
                for (int i = 0; i < ctx.getParent().getChildCount(); i++) {
                    Log.d(TAG, "enterTable_name() child #" + i + " = " + ctx.getParent().getChild(i).getText());
                }
                super.enterTable_name(ctx);
            }

            @Override
            public void enterTable_alias(@NotNull SQLiteParser.Table_aliasContext ctx) {
                Log.d(TAG, "==================================");
                Log.d(TAG, "enterTable_alias() called with: " + "ctx = [" + ctx.getText() + "]"+ "#" + + ctx.getStart().getStartIndex() + "-" + ctx.getStop().getStopIndex()
                        + " | parent = [" + ctx.getParent().getText() + "]");
                for (int i = 0; i < ctx.getParent().getChildCount(); i++) {
                    Log.d(TAG, "enterTable_alias() child #" + i + " = " + ctx.getParent().getChild(i).getText());
                }
                super.enterTable_alias(ctx);
            }

            @Override
            public void enterColumn_name(@NotNull SQLiteParser.Column_nameContext ctx) {
                Log.d(TAG, "==================================");
                Log.d(TAG, "enterColumn_name() called with: " + "ctx = [" + ctx.getText() + "]"+ "#" + ctx.getStart().getStartIndex() + "-" + ctx.getStop().getStopIndex()
                    + " | parent = [" + ctx.getParent().getText() + "]");
                for (int i = 0; i < ctx.getParent().getChildCount(); i++) {
                    Log.d(TAG, "enterColumn_name() child #" + i + " = " + ctx.getParent().getChild(i).getText());
                }
                super.enterColumn_name(ctx);
            }

            @Override
            public void enterColumn_alias(@NotNull SQLiteParser.Column_aliasContext ctx) {
                Log.d(TAG, "==================================");
                Log.d(TAG, "enterColumn_alias() called with: " + "ctx = [" + ctx.getText() + "]"+ "#"+ ctx.getStart().getStartIndex() + "-" + ctx.getStop().getStopIndex()
                        + " | parent = [" + ctx.getParent().getText() + "]");
                for (int i = 0; i < ctx.getParent().getChildCount(); i++) {
                    Log.d(TAG, "enterColumn_alias() child #" + i + " = " + ctx.getParent().getChild(i).getText());
                }
                super.enterColumn_alias(ctx);
            }
        }, tree);

        // Print the parsed functions.
        Log.d(TAG, "testSQLiteParser() returned: " + functionNames);
    }
}
