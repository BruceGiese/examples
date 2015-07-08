package com.brucegiese.testingactiveandroid;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name="Bar")
public class Bar extends Model {

    @Column(name="User")
    public String mUser;


    @SuppressWarnings("unused")
    public Bar() {
        super();
    }

    public Bar( String user ) {
        super();
        mUser = user;
    }

}
