package com.mordred.aero.verification

import com.mordred.aero.theme.AeroColorScheme
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * API-01 / API-04 / T-22-32: no public library declaration may expose a `com.sun.jna` type.
 * JNA is an `implementation`-scope dependency of `:library`; this gate loads every compiled
 * main class under `com/mordred/aero` (located through the code source of a known main class,
 * so it scans the real artifact bytes, not the sources) and inspects every public class
 * outside `internal/windows` — its public methods (parameters, return and generic types),
 * constructors and fields — failing with a list of every signature that mentions
 * `com.sun.jna`.
 */
class PublicApiNoJnaTest {

    @Test
    fun noPublicLibrarySignatureMentionsComSunJna() {
        val anchor = AeroColorScheme::class.java
        val root = Paths.get(anchor.protectionDomain.codeSource.location.toURI())
        val violations = mutableListOf<String>()

        Files.walk(root).use { stream ->
            stream.filter { it.toString().endsWith(".class") }
                .sorted()
                .forEach { path ->
                    val relative = root.relativize(path).toString().replace('\\', '/')
                    if (!relative.startsWith("com/mordred/aero/")) return@forEach
                    // The Windows-only internals legitimately use JNA; everything else is the
                    // public surface (Kotlin `internal` compiles to public bytecode and is
                    // therefore covered too — it must not leak JNA either).
                    if (relative.contains("internal/windows")) return@forEach
                    val fqcn = relative.removeSuffix(".class").replace('/', '.')
                    val cls = Class.forName(fqcn, false, anchor.classLoader)
                    if (!Modifier.isPublic(cls.modifiers)) return@forEach

                    fun check(signature: String, what: String) {
                        if (signature.contains("com.sun.jna")) {
                            violations.add("$fqcn $what: $signature")
                        }
                    }
                    for (method in cls.methods) check(method.toGenericString(), "method")
                    for (constructor in cls.constructors) check(constructor.toGenericString(), "constructor")
                    for (field in cls.fields) check(field.toGenericString(), "field")
                }
        }

        assertTrue(
            violations.isEmpty(),
            "API-01/API-04: no public library declaration may expose com.sun.jna in its " +
                "signature — JNA must stay implementation-scoped. Found ${violations.size} " +
                "violation(s):\n${violations.joinToString("\n")}",
        )
    }
}
