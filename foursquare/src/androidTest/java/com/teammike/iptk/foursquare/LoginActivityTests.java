package com.teammike.iptk.foursquare;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.teammike.iptk.foursquare.activities.LoginActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTests {

    @Rule
    public ActivityTestRule<LoginActivity> loginActivityActivityTestRule =
            new ActivityTestRule<>(LoginActivity.class);

    @Test
    public void userLoginWithValidCredentials_opensOverviewActivity() {
        // given
        onView(withId(R.id.email))
                .perform(typeText("yadullah@duman.com")); // user object should be in the db
        onView(withId(R.id.password))
                .perform(typeText("foobar"));

        // when
        onView(withId(R.id.email_sign_in_button))
                .perform(click());

        // then
        onView(withId(R.id.title))
                .check(matches(isDisplayed()));

    }

    @Test
    public void userLoginWithInvalidCredentials_showsToastMessage() {
        // given
        onView(withId(R.id.email))
                .perform(typeText("foo@foo.com"));
        onView(withId(R.id.password))
                .perform(typeText("foobarfoobar"));

        // when
        onView(withId(R.id.email_sign_in_button))
                .perform(click());

        // then
        onView(withText(R.string.error_invalid_login))
                .inRoot(withDecorView(not(is(loginActivityActivityTestRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }


}
