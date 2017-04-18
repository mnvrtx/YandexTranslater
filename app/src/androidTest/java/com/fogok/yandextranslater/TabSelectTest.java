package com.fogok.yandextranslater;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class TabSelectTest {

    @Rule
    public ActivityTestRule<TabSelect> mActivityTestRule = new ActivityTestRule<>(TabSelect.class);

    @Test
    public void tabSelectTest() {
        ViewInteraction viewPager = onView(
                allOf(withId(R.id.container),
                        withParent(allOf(withId(R.id.main_content),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        viewPager.perform(swipeLeft());

        ViewInteraction appCompatTextView2 = onView(
                allOf(withText("ИЗБРАННОЕ"), isDisplayed()));
        appCompatTextView2.perform(click());

        ViewInteraction appCompatTextView3 = onView(
                allOf(withText("ИСТОРИЯ"), isDisplayed()));
        appCompatTextView3.perform(click());

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.searchEditText), isDisplayed()));
        appCompatEditText.perform(replaceText("qw"), closeSoftKeyboard());

        ViewInteraction appCompatTextView4 = onView(
                allOf(withText("ИЗБРАННОЕ"), isDisplayed()));
        appCompatTextView4.perform(click());

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.searchEditText), isDisplayed()));
        appCompatEditText2.perform(click());

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.searchEditText), isDisplayed()));
        appCompatEditText3.perform(replaceText("gg"), closeSoftKeyboard());

        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.clearButton),
                        withParent(withId(R.id.constraintLayout2)),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction appCompatButton = onView(
                allOf(withId(android.R.id.button1), withText("Удалить все элементы")));
        appCompatButton.perform(scrollTo(), click());

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withId(R.id.clearButton),
                        withParent(withId(R.id.constraintLayout2)),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(android.R.id.button2), withText("Удалить всё, кроме избранного")));
        appCompatButton2.perform(scrollTo(), click());

    }

}
