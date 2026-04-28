package com.mordred.aero.components.overlay

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AeroToastHostStateTest {

    @Test
    fun showToastAppendsToStack() = runTest {
        val state = AeroToastHostState()
        val deferred = async { state.showToast("hello") }
        // Allow the coroutine to enter showToast
        yield()
        assertEquals(1, state.toasts.size, "showToast must append a toast")
        assertEquals("hello", state.toasts[0].message)
        // Dismiss to let the deferred resume — otherwise runTest reports leaked coroutine
        state.dismiss(state.toasts[0].id, AeroToastResult.Dismissed)
        val result = deferred.await()
        assertEquals(AeroToastResult.Dismissed, result)
    }

    @Test
    fun stackEvictsOldestWhenSixthAdded() = runTest {
        val state = AeroToastHostState()
        val deferreds = (0 until 5).map { idx ->
            async { state.showToast("toast-$idx") }
        }
        yield()
        assertEquals(5, state.toasts.size)
        // Add a 6th — first one must be evicted
        val sixth = async { state.showToast("toast-5") }
        yield()
        assertEquals(5, state.toasts.size, "stack must stay capped at MAX_STACK_SIZE")
        // The oldest (toast-0) was evicted; current contents start at toast-1
        assertEquals("toast-1", state.toasts.first().message)
        // The deferred for toast-0 must have completed with Evicted
        val evictedResult = deferreds[0].await()
        assertEquals(AeroToastResult.Evicted, evictedResult)
        // Cleanup remaining
        state.toasts.toList().forEach { state.dismiss(it.id, AeroToastResult.Dismissed) }
        deferreds.drop(1).forEach { it.await() }
        sixth.await()
    }

    @Test
    fun dismissCallbackResumesShowToastWithDismissed() = runTest {
        val state = AeroToastHostState()
        val deferred = async { state.showToast("x", AeroToastDuration.Indefinite) }
        yield()
        val id = state.toasts[0].id
        state.dismiss(id, AeroToastResult.Dismissed)
        val result = deferred.await()
        assertEquals(AeroToastResult.Dismissed, result)
        assertTrue(state.toasts.isEmpty())
    }

    @Test
    fun maxStackSizeIs5() {
        assertEquals(5, AeroToastHostState.MAX_STACK_SIZE)
    }
}
