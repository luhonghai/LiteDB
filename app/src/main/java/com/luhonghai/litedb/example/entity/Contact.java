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

package com.luhonghai.litedb.example.entity;

import com.luhonghai.litedb.LiteColumnType;
import com.luhonghai.litedb.annotation.LiteColumn;
import com.luhonghai.litedb.annotation.LiteTable;
import com.luhonghai.litedb.LiteEntity;

import java.util.Date;

/**
 * Created by luhonghai on 9/7/15.
 */
@LiteTable
public class Contact extends LiteEntity {

    @LiteColumn(name = "contact_name", isNotNull = true)
    private String name;

    @LiteColumn(defaultValue = "VN84000000000")
    private String phone;

    @LiteColumn(alias = "extra_job")
    private String job;

    @LiteColumn
    private float salary;

    @LiteColumn
    private double balance;

    @LiteColumn
    private int age;

    @LiteColumn
    private Date createdDate;

    @LiteColumn(dateColumnType = LiteColumnType.INTEGER)
    private Date createdDate1;

    @LiteColumn(dateColumnType = LiteColumnType.REAL)
    private Date createdDate2;

    @LiteColumn(dateColumnType = LiteColumnType.TEXT)
    private Date createdDate3;

    @LiteColumn
    private BlobData blobData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getCreatedDate1() {
        return createdDate1;
    }

    public void setCreatedDate1(Date createdDate1) {
        this.createdDate1 = createdDate1;
    }

    public Date getCreatedDate2() {
        return createdDate2;
    }

    public void setCreatedDate2(Date createdDate2) {
        this.createdDate2 = createdDate2;
    }

    public Date getCreatedDate3() {
        return createdDate3;
    }

    public void setCreatedDate3(Date createdDate3) {
        this.createdDate3 = createdDate3;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public float getSalary() {
        return salary;
    }

    public void setSalary(float salary) {
        this.salary = salary;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public BlobData getBlobData() {
        return blobData;
    }

    public void setBlobData(BlobData blobData) {
        this.blobData = blobData;
    }
}
