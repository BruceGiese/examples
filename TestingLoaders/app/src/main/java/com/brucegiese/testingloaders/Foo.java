package com.brucegiese.testingloaders;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.io.Serializable;


@Table(name="Foo", id = BaseColumns._ID)
public class Foo extends Model implements Serializable {

    @Column( name= "a")
    public String mA;

    @Column( name = "b")
    public int mB;

    public Foo() {
        super();
    }

    public Foo(String a, int b) {
        super();
        mA = a;
        mB = b;
    }


    public String getA() {
        return mA;
    }
    public void setA(String a) {
        mA = a;
    }

    public int getB() {
        return mB;
    }
    public void setB(int b) {
        mB = b;
    }
}
