package com.brucegiese.testingactiveandroid;


import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;


@Table(name="Follow")
public class Follow extends Model {

    @Column(name="Follower", onDelete= Column.ForeignKeyAction.CASCADE)
    public Foo mFollower;

    @Column(name="Followed", onDelete= Column.ForeignKeyAction.CASCADE)
    public Foo mFollowed;


    @SuppressWarnings("unused")
    public Follow() {
        super();
    }

    public Follow(Foo follower, Foo followed) {
        super();
        mFollower = follower;
        mFollowed = followed;
    }

}
