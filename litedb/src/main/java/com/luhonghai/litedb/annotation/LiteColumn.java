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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by luhonghai on 07/09/15.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface  LiteColumn {
    /**
     * Field is primary key
     */
    boolean isPrimaryKey() default false;

    /**
     * Autoincrement. Support only number field
     */
    boolean isAutoincrement() default false;

    /**
     * Not allow null
     */
    boolean isNotNull() default false;

    /** Column name. */
    String name() default "";

    /**
     * Column alias
     */
    String alias() default "";

    /**
     * Simple default values
     * Input value must be valid SQLite value type
     * For example:
     *
     * Long column: 1
     * String column: 'String value'
     * Boolean column: 1 or 0
     */
    String defaultValue() default "";

    /**
     * Only work with field type java.util.Date
     *
     * SQLite does not have a storage class set aside for storing dates and/or times.
     * Instead, the built-in Date And Time Functions of SQLite are capable of storing dates and times as TEXT, REAL, or INTEGER values:
     * TEXT as ISO8601 strings ("YYYY-MM-DD HH:MM:SS.SSS").
     * REAL as Julian day numbers, the number of days since noon in Greenwich on November 24, 4714 B.C. according to the proleptic Gregorian calendar.
     * INTEGER as Unix Time, the number of seconds since 1970-01-01 00:00:00 UTC.
     * See more https://www.sqlite.org/datatype3.html
     */
    LiteColumnType dateColumnType() default LiteColumnType.INTEGER;
}
