package com.campus.network.core.network

import android.content.SharedPreferences
import io.github.jan.supabase.gotrue.user.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Unit tests for [SupabaseSessionStore] and [PostgrestHttpException].
 * Pure JVM JUnit tests using an in-memory fake [SharedPreferences].
 */
class SupabaseSessionStoreTest {

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        SupabaseSessionStore.initializeForTesting(fakePrefs)
    }

    private fun createSampleJwt(expiresInSeconds: Long = 3600): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray(Charsets.UTF_8))
        val exp = (System.currentTimeMillis() / 1000) + expiresInSeconds
        val payload = encoder.encodeToString("""{"sub":"test-user-id","exp":$exp,"role":"authenticated"}""".toByteArray(Charsets.UTF_8))
        val signature = encoder.encodeToString("mock-signature".toByteArray(Charsets.UTF_8))
        return "$header.$payload.$signature"
    }

    @Test
    fun postgrestHttpException_isTransientSemantics() {
        // 5xx server errors should be marked transient
        assertTrue(PostgrestHttpException(500, "Internal Server Error").isTransient)
        assertTrue(PostgrestHttpException(502, "Bad Gateway").isTransient)
        assertTrue(PostgrestHttpException(503, "Service Unavailable").isTransient)
        assertTrue(PostgrestHttpException(504, "Gateway Timeout").isTransient)

        // 429 Too Many Requests should be marked transient (rate limited, retry later)
        assertTrue(PostgrestHttpException(429, "Too Many Requests").isTransient)

        // 4xx client errors should be marked permanent (not transient)
        assertFalse(PostgrestHttpException(400, "Bad Request").isTransient)
        assertFalse(PostgrestHttpException(401, "Unauthorized").isTransient)
        assertFalse(PostgrestHttpException(403, "Forbidden").isTransient)
        assertFalse(PostgrestHttpException(404, "Not Found").isTransient)
        assertFalse(PostgrestHttpException(409, "Conflict").isTransient)
        assertFalse(PostgrestHttpException(422, "Unprocessable Entity").isTransient)
    }

    @Test
    fun sessionStore_saveAndLoadSdkSession() {
        val jwt = createSampleJwt(3600)
        val user = AuthenticatedSessionUser(
            userId = "test-uid-123",
            email = "test@campus.ac.ke",
            fullName = "Lincoln Paul",
            avatarUrl = "https://example.com/avatar.jpg",
            accessToken = jwt,
            refreshToken = "sample-refresh-token-xyz"
        )

        SupabaseSessionStore.save(user)

        val sdkSession = SupabaseSessionStore.loadSdkSession()
        assertNotNull(sdkSession)
        assertEquals(jwt, sdkSession!!.accessToken)
        assertEquals("sample-refresh-token-xyz", sdkSession.refreshToken)
        assertEquals("bearer", sdkSession.tokenType)
        assertTrue("ExpiresIn should be positive for active token", sdkSession.expiresIn > 0L)
    }

    @Test
    fun sessionStore_clearRemovesSession() {
        val jwt = createSampleJwt(1800)
        SupabaseSessionStore.save(
            AuthenticatedSessionUser(
                userId = "test-uid-456",
                email = "user@campus.ac.ke",
                fullName = "Lincoln Paul",
                avatarUrl = null,
                accessToken = jwt,
                refreshToken = "refresh-xyz"
            )
        )
        assertNotNull(SupabaseSessionStore.loadSdkSession())

        SupabaseSessionStore.clear()
        assertNull(SupabaseSessionStore.loadSdkSession())
        assertNull(SupabaseSessionStore.accessToken())
    }

    @Test
    fun sessionStore_saveSdkSessionUpdatesTokens() {
        val newJwt = createSampleJwt(7200)
        val rotatedSession = UserSession(
            accessToken = newJwt,
            refreshToken = "rotated-refresh-token-999",
            expiresIn = 7200,
            tokenType = "bearer",
            user = null
        )

        SupabaseSessionStore.saveSdkSession(rotatedSession)

        val loaded = SupabaseSessionStore.loadSdkSession()
        assertNotNull(loaded)
        assertEquals(newJwt, loaded!!.accessToken)
        assertEquals("rotated-refresh-token-999", loaded.refreshToken)
    }

    /**
     * Minimal in-memory FakeSharedPreferences for hermetic JVM unit testing.
     */
    private class FakeSharedPreferences : SharedPreferences {
        private val storage = ConcurrentHashMap<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(storage)
        override fun getString(key: String?, defValue: String?): String? = (storage[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (storage[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (storage[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (storage[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (storage[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (storage[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = storage.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(storage)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val backing: ConcurrentHashMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = HashMap<String, Any?>()
            private val toRemove = HashSet<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { key?.let { pending[it] = values } }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { key?.let { pending[it] = value } }
            override fun remove(key: String?): SharedPreferences.Editor = apply { key?.let { toRemove.add(it) } }
            override fun clear(): SharedPreferences.Editor = apply { clearAll = true }

            override fun commit(): Boolean {
                if (clearAll) backing.clear()
                toRemove.forEach { backing.remove(it) }
                pending.forEach { (k, v) -> if (v != null) backing[k] = v else backing.remove(k) }
                return true
            }

            override fun apply() { commit() }
        }
    }
}
