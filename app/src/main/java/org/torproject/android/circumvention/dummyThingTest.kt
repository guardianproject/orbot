package org.torproject.android.circumvention

import org.junit.Test
import org.junit.Assert

internal class DummyThingTest {
    @Test
    @Throws(Exception::class)
    fun additionFailingThing() {
        assert(false)
    }

    @Test
    @Throws(Exception::class)
    fun additionSucceedingThing() {
        assert(true)
    }

    @Test
    @Throws(Exception::class)
    fun additionAddsWrongButShouldGiveInformativeAssertionFailure() {
        val dt = DummyThing()
        Assert.assertEquals(dt.add(1,2), 2)
    }
}