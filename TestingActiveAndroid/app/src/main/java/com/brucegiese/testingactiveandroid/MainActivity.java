package com.brucegiese.testingactiveandroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;

import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = "AAtest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActiveAndroid.initialize(this);


        Foo foo1 = new Foo("User One");
        foo1.save();
        Foo foo2 = new Foo("User Two");
        foo2.save();
        Foo foo3 = new Foo("User Three");
        foo3.save();
        Foo foo4 = new Foo("User Four");
        foo4.save();

        Bar bar2 = new Bar("User Bar2");
        bar2.save();
        Bar bar3 = new Bar("User Bar3");
        bar3.save();
        Bar bar4 = new Bar("User Bar4");
        bar4.save();

        Follow follow1 = new Follow( foo1, foo2);
        follow1.save();

        Follow follow2 = new Follow( foo1, foo3);
        follow2.save();

        Follow follow3 = new Follow( foo1, foo4);
        follow3.save();

        Follow follow4 = new Follow( foo4, foo1);
        follow4.save();

        Follow follow5 = new Follow( foo3, foo2);
        follow5.save();


        checkBoth(foo1.getId(), foo2.getId());
        checkFollower(foo1.getId());
        checkFollowed(foo2.getId());

        Log.i(TAG, "Deleting foo4,foo1 follow");
        follow4.delete();
        Log.i(TAG, "Deleting foo3, foo2 follow");
        follow5.delete();

        checkBoth(foo1.getId(), foo2.getId());
        checkFollower(foo1.getId());
        checkFollowed(foo2.getId());

    }




    void checkBoth(long follower, long followed) {

        Log.i(TAG, "Fetching where follower = " + follower + " AND followed = " + followed);

        List<Follow> followList = new Select()
                .from(Follow.class)
                .where("Follower = ? AND Followed = ?", follower, followed)
                .execute();

        for( Follow f : followList ) {
            Log.i(TAG, "... " + f.mFollower.mUser + ", " + f.mFollowed.mUser);
        }
    }

    void checkFollower(long follower) {

        Log.i(TAG, "Fetching where follower = " + follower);

        List<Follow> followList = new Select()
                .from(Follow.class)
                .where("Follower = ?", follower)
                .execute();

        for( Follow f : followList ) {
            Log.i(TAG, "... " + f.mFollower.mUser + ", " + f.mFollowed.mUser);
        }
    }

    void checkFollowed(long followed) {

        Log.i(TAG, "Fetching where followeD = " + followed);

        List<Follow> followList = new Select()
                .from(Follow.class)
                .where("Followed = ?", followed)
                .execute();

        for( Follow f : followList ) {
            Log.i(TAG, "... " + f.mFollower.mUser + ", " + f.mFollowed.mUser);
        }
    }

}
