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
package com.luhonghai.litedb.example.db;

import android.content.Context;

import com.luhonghai.litedb.LiteDatabaseHelper;
import com.luhonghai.litedb.annotation.LiteDatabase;
import com.luhonghai.litedb.example.entity.Contact;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;

/**
 * Created by luhonghai on 07/09/15.
 */
@LiteDatabase(tables = {Contact.class})
public class MainDatabaseHelper extends LiteDatabaseHelper {
    /**
     * Construct database service with context of the application.
     *
     * @param context the Context within which to work
     */
    public MainDatabaseHelper(Context context) throws AnnotationNotFound, InvalidAnnotationData {
        super(context);
    }

    public MainDatabaseHelper(Context context, DatabaseListener databaseListener) throws AnnotationNotFound, InvalidAnnotationData {
        super(context, databaseListener);
    }
}
