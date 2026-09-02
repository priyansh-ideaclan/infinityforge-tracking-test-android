package com.factory.core.common.auth

import com.factory.core.common.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAuthRepositoryTest {

    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() {
        repository = FakeAuthRepository(idGenerator = SequentialIdGenerator())
    }

    @Test
    fun `initial state is signed out`() {
        assertEquals(AuthState.SignedOut, repository.authState.value)
    }

    @Test
    fun `register then sign in with same credentials succeeds`() = runTest {
        val registerResult = repository.registerWithEmail("a@example.com", "password123")
        assertTrue(registerResult is AppResult.Success)
        repository.signOut()

        val signInResult = repository.signInWithEmail("a@example.com", "password123")

        assertTrue(signInResult is AppResult.Success)
        assertTrue(repository.authState.value is AuthState.SignedIn)
    }

    @Test
    fun `sign in with wrong password fails`() = runTest {
        repository.registerWithEmail("a@example.com", "password123")
        repository.signOut()

        val result = repository.signInWithEmail("a@example.com", "wrong-password")

        assertTrue(result is AppResult.Failure)
        assertEquals(AuthState.SignedOut, repository.authState.value)
    }

    @Test
    fun `registering the same email twice fails`() = runTest {
        repository.registerWithEmail("a@example.com", "password123")

        val second = repository.registerWithEmail("a@example.com", "password456")

        assertTrue(second is AppResult.Failure)
    }

    @Test
    fun `sign in anonymously succeeds and marks user anonymous`() = runTest {
        val result = repository.signInAnonymously()

        assertTrue(result is AppResult.Success)
        val user = (result as AppResult.Success).data
        assertTrue(user.isAnonymous)
    }

    @Test
    fun `sign out returns to signed out state`() = runTest {
        repository.signInAnonymously()

        repository.signOut()

        assertEquals(AuthState.SignedOut, repository.authState.value)
    }

    @Test
    fun `password reset for unknown email fails`() = runTest {
        val result = repository.sendPasswordReset("unknown@example.com")

        assertTrue(result is AppResult.Failure)
    }
}

private class SequentialIdGenerator : com.factory.core.common.IdGenerator {
    private var counter = 0
    override fun newId(): String = "user-${++counter}"
}
