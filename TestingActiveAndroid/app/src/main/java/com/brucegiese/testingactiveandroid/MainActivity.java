package com.brucegiese.testingactiveandroid;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.query.Select;

import java.util.List;


public class MainActivity extends Activity {

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

//        Follow follow1 = new Follow( foo1, bar2);
        Follow follow1 = new Follow( foo1, foo2);
        follow1.save();

//        Follow follow2 = new Follow( foo1, bar3);
        Follow follow2 = new Follow( foo1, foo3);
        follow2.save();

//        Follow follow3 = new Follow( foo1, bar4);
        Follow follow3 = new Follow( foo1, foo4);
        follow3.save();



        List<Follow> followList = new Select()
                .from(Follow.class)
                .where("Follower = ? AND Followed = ?", foo1.getId(), foo2.getId())
//                .where("Follower = ? AND Followed = ?", foo1.getId(), bar2.getId())
                .execute();

        if( followList.size() != 1 ) {
            Toast.makeText(this, "We got " + followList.size() + " Follow objects back", Toast.LENGTH_LONG).show();
        } else {
            if( followList.get(0).mFollower.equals(foo1) ) {
                Toast.makeText(this, "Success!!!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "We got the wrong Follow: " + followList.get(0).mFollower.mUser
                        + ", " + followList.get(0).mFollowed.mUser, Toast.LENGTH_LONG).show();
            }
        }


    }


}
