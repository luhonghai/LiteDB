/*
 * Copyright (c) 2015. luhonghai@luhonghai.com
 */

package com.halosolutions.litedb;

import android.content.Context;

import com.halosolutions.litedb.exception.AnnotationNotFound;
import com.halosolutions.litedb.exception.InvalidAnnotationData;

/**
 * Created by luhonghai on 07/09/15.
 */
public class TestDatabaseHelper extends LiteBaseDatabase {
    /**
     * Construct database service with context of the application.
     *
     * @param context the Context within which to work
     */
    public TestDatabaseHelper(Context context) throws AnnotationNotFound, InvalidAnnotationData {
        super(context);
    }
}
