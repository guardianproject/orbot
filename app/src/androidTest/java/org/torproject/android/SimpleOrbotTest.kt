import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.torproject.android.R
import org.torproject.android.OrbotActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class SimpleOrbotTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(OrbotActivity::class.java)

    @Test
    fun simpleOrbotTest() {
        val appCompatTextView = onView(
                allOf(withId(R.id.tvConfigure), withText("Configure"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.drawerLayout),
                                        0),
                                7),
                        isDisplayed()))
        appCompatTextView.perform(click())

        val textView = onView(
                allOf(withText("Get a Bridge from Tor (Obfs4)"),
                        withParent(withParent(withId(R.id.requestContainer))),
                        isDisplayed()))
        textView.check(matches(withText("Get a Bridge from Tor (Obfs4)")))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
