package com.brucegiese.testingloaders;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.io.Serializable;


@Table(name="Bar",id=BaseColumns._ID)
public class Bar extends Model implements Serializable {

    @Column( name= "x")
    public String mX;

    @Column( name = "y")
    public int mY;

    public Bar() {
        super();
    }

    public Bar(String x, int y) {
        super();
        mX = x;
        mY = y;
    }


    public String getX() {
        return mX;
    }
    public void setX(String x) {
        mX = x;
    }

    public int getY() {
        return mY;
    }
    public void setY(int y) {
        mY = y;
    }
}
