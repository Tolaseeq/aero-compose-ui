package com.mordred.aero.internal.windows

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WIN-06 / PITFALLS 6: headless locks for the copy-on-write snapshot bridge between the Compose
 * EDT and the native WndProc thread. Every publish swaps a whole new immutable snapshot into the
 * [java.util.concurrent.atomic.AtomicReference]; the native side's plain volatile read can
 * therefore observe at most a one-publish-stale snapshot, never a torn rect and never a
 * snapshot that changes after it was returned.
 */
class HitTestRegionRegistryTest {

    /**
     * PITFALLS 6 / T-22-19: one writer hammering role + interactive publishes while four readers
     * snapshot concurrently. Every rect the readers observe must be internally consistent
     * (left == top and right == left + n — an interleaved read of two different publishes would
     * break the invariant), and every retained snapshot must still read the same afterwards.
     */
    @Test
    fun concurrentPublishAndReadNeverTearsARect() {
        val registry = HitTestRegionRegistry()
        val violations = ConcurrentLinkedQueue<String>()
        val writerDone = AtomicBoolean(false)
        val retained = ConcurrentLinkedQueue<Pair<HitTestSnapshot, String>>()

        // Seed the role before readers start so "role present" holds throughout.
        registry.publishRole(TitleBarRole.Maximize, PxRect(0, 0, 1, 1))

        val readers = (1..4).map {
            Thread {
                var seen = 0L
                while (!writerDone.get()) {
                    val snap = registry.snapshot()
                    seen++
                    val role = snap.roles[TitleBarRole.Maximize]
                    if (role == null) {
                        violations.add("role vanished during an active writer: $snap")
                    } else if (!(role.left == role.top && role.right == role.left + 1 && role.bottom == role.top + 1)) {
                        violations.add("torn role rect: $role")
                    }
                    for (rect in snap.interactive.values) {
                        if (!(rect.left == rect.top && rect.right == rect.left + 3 && rect.bottom == rect.top + 3)) {
                            violations.add("torn interactive rect: $rect")
                        }
                    }
                    if (seen % 10_000L == 0L) {
                        // A returned snapshot must never change afterwards — retain it with a
                        // serialization of its contents and re-check after the writer finishes.
                        retained.add(snap to snap.toString())
                    }
                }
            }
        }

        val writer = Thread {
            for (k in 0..200_000) {
                registry.publishRole(TitleBarRole.Maximize, PxRect(k, k, k + 1, k + 1))
                if (k % 7 == 0) registry.publishInteractive(9L, PxRect(k, k, k + 3, k + 3))
                if (k % 7 == 1) registry.publishInteractive(9L, null)
            }
            writerDone.set(true)
        }

        writer.start()
        readers.forEach { it.start() }
        writer.join()
        readers.forEach { it.join() }

        assertTrue(violations.isEmpty(), "concurrent read violations:\n${violations.joinToString("\n")}")
        assertTrue(retained.isNotEmpty(), "the readers must have retained at least one snapshot")
        retained.forEach { (snap, frozen) ->
            assertEquals(frozen, snap.toString(), "a returned snapshot changed afterwards")
        }
        assertEquals(
            PxRect(200_000, 200_000, 200_001, 200_001),
            registry.snapshot().roles[TitleBarRole.Maximize],
            "the final publish is the visible one",
        )
    }

    /** WIN-06: two windows' registries are fully independent — no cross-window leakage. */
    @Test
    fun twoRegistriesNeverSeeEachOthersRects() {
        val windowA = HitTestRegionRegistry()
        val windowB = HitTestRegionRegistry()

        windowA.publishRole(TitleBarRole.Maximize, PxRect(0, 0, 46, 32))
        windowA.publishCaption(1L, PxRect(8, 0, 1192, 32))
        assertEquals(HitTestSnapshot.EMPTY, windowB.snapshot())

        windowB.publishInteractive(5L, PxRect(100, 0, 140, 32))
        assertEquals(1, windowA.snapshot().captions.size)
        assertEquals(1, windowA.snapshot().roles.size)
        assertTrue(windowA.snapshot().interactive.isEmpty())
        assertEquals(1, windowB.snapshot().interactive.size)
        assertTrue(windowB.snapshot().captions.isEmpty())
    }

    /**
     * BTN-02 / API-04: a null publish removes the entry (a disposed element stops claiming
     * pixels), and caption / interactive ids share one strictly increasing per-registry
     * sequence, so every allocated id is distinct.
     */
    @Test
    fun nullPublishRemovesEntriesAndIdsShareOneSequence() {
        val registry = HitTestRegionRegistry()

        registry.publishRole(TitleBarRole.Maximize, PxRect(0, 0, 46, 32))
        registry.publishRole(TitleBarRole.Maximize, null)
        assertTrue(registry.snapshot().roles.isEmpty())

        val captionId = registry.newCaptionId()
        registry.publishCaption(captionId, PxRect(8, 0, 1192, 32))
        assertEquals(setOf(captionId), registry.snapshot().captions.keys)
        registry.publishCaption(captionId, null)
        assertTrue(registry.snapshot().captions.isEmpty())

        val interactiveId = registry.newInteractiveId()
        registry.publishInteractive(interactiveId, PxRect(120, 0, 160, 32))
        assertEquals(setOf(interactiveId), registry.snapshot().interactive.keys)
        registry.publishInteractive(interactiveId, null)
        assertTrue(registry.snapshot().interactive.isEmpty())

        val ids = listOf(registry.newCaptionId(), registry.newInteractiveId(), registry.newCaptionId())
        assertEquals(3, ids.toSet().size, "ids are distinct across the two maps")
        assertTrue(ids.zipWithNext().all { (a, b) -> a < b }, "ids come from one increasing sequence")
    }
}
