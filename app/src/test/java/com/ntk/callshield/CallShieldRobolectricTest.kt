package com.ntk.callshield

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CallShieldRobolectricTest {

    @Test
    fun `application name matches NTK CallShield`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("NTK CallShield", context.getString(R.string.app_name))
        assertEquals("Know before you answer.", context.getString(R.string.app_tagline))
    }
}
