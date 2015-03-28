package com.brucegiese.perfectposture;


import android.annotation.SuppressLint;
import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.Date;

@SuppressWarnings("FieldCanBeLocal")
@Table(name="Sample", id= BaseColumns._ID)
class Sample extends Model {
    @Column(name="Value")
    public int value;

    @SuppressLint("all")
    public Sample() {       // This constructor is mandatory.  Don't remove it.
        super();
    }

    // Don't rely on the date as a key, due to time zone changes, daylight savings time changes...
    @Column(name="Date")
    private Date date;

    // Whether this data point is considered good posture or bad posture
    @Column(name="GoodPosture")
    private boolean goodPosture;

    public Sample(int value, Date date, boolean goodPosture) {
        this.value = value;
        this.date = date;
        this.goodPosture = goodPosture;
    }
}