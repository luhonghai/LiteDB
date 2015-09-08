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

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.luhonghai.litedb.example.db.ContactDao;
import com.luhonghai.litedb.example.db.MainDatabaseHelper;
import com.luhonghai.litedb.example.entity.BlobData;
import com.luhonghai.litedb.example.entity.Contact;
import com.luhonghai.litedb.exception.AnnotationNotFound;
import com.luhonghai.litedb.exception.InvalidAnnotationData;
import com.luhonghai.litedb.exception.LiteDatabaseException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    public ApplicationTest() {
        super(Application.class);
    }

    /**
     * Just a very simple test
     * @throws AnnotationNotFound
     * @throws InvalidAnnotationData
     * @throws LiteDatabaseException
     */
    public void testLiteDatabase() throws LiteDatabaseException, AnnotationNotFound, InvalidAnnotationData {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm.SSS", Locale.getDefault());
        MainDatabaseHelper databaseHelper = new MainDatabaseHelper(getContext(), new LiteDatabaseHelper.DatabaseListener() {
            @Override
            public void onBeforeDatabaseCreate(SQLiteDatabase db) {

            }

            @Override
            public void onAfterDatabaseCreate(SQLiteDatabase db) {

            }

            @Override
            public void onBeforeDatabaseUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }

            @Override
            public void onAfterDatabaseUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }

            @Override
            public void onError(SQLiteDatabase db, final String message, Throwable throwable) {
                Log.e("TESTDB", "onError ", throwable);
                assertEquals("", message);
            }
        });
        ContactDao contactDao = new ContactDao(databaseHelper);
        // Try to open database
        contactDao.open();
        // Try to delete all current records
        if (databaseHelper.isTableExists(contactDao.getAnnotationHelper().getTableName()))
            contactDao.deleteAll();
        Contact contact = new Contact();
        contact.setName("1st Name");
        contact.setCreatedDate(new Date(System.currentTimeMillis()));
        contact.setCreatedDate1(contact.getCreatedDate());
        contact.setCreatedDate2(contact.getCreatedDate());
        // Try to insert new record
        long id = contactDao.insert(contact);
        // Try to get count
        assertEquals(1, contactDao.count("[name] = ?", new String[]{"1st Name"}));
        // Try to get record by id
        Contact refContact = contactDao.get(id);
        assertEquals(refContact.getName(), contact.getName());
        assertEquals(refContact.getCreatedDate3(), null);
        // less than 2 millisecond difference is okay
        assertTrue(Math.abs(contact.getCreatedDate().getTime() - refContact.getCreatedDate().getTime()) <= 2);
        // less than 2 millisecond difference is okay
        assertTrue(Math.abs(contact.getCreatedDate().getTime() - refContact.getCreatedDate1().getTime()) <= 2);
        // less than 2 millisecond difference is okay
        assertTrue(Math.abs(contact.getCreatedDate().getTime() - refContact.getCreatedDate2().getTime()) <= 2);
        refContact.setName("2nd Name");
        refContact.setJob("Job");
        refContact.setAge(26);
        refContact.setSalary(3000.0f);
        refContact.setBalance(Double.MAX_VALUE);
        BlobData blobData = new BlobData(UUID.randomUUID().toString());
        refContact.setBlobData(blobData);
        // Try to update record
        contactDao.update(refContact);
        // Try to list all record
        List<Contact> contactList = contactDao.listAll();
        for (Contact mContact : contactList) {
            assertEquals("VN84000000000", mContact.getPhone());
            assertEquals("2nd Name", mContact.getName());
            assertEquals("Job", mContact.getJob());
            assertEquals(Double.MAX_VALUE, mContact.getBalance());
            assertEquals(3000.0f, mContact.getSalary());
            assertEquals(26, mContact.getAge());
            assertEquals(blobData, mContact.getBlobData());
        }

        contactDao.deleteByKey(id);
        assertEquals(0, contactDao.count());
        contactDao.close();
    }
}