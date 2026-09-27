import java.util.concurrent.atomic.AtomicLong

// TOOL-16: tests executed on the pre-upgrade toolchain (Kotlin 2.4.10 / CMP 1.11.1), measured by
// an actual `./gradlew :library:test --rerun` run immediately before the first version bump. The
// guard in tasks.test below fails the build on any other number; change only with a commit that
// states why.
// 577 = 541 pre-phase + 36 native-window logic tests (VER-14, Phase 22)
// 587 = 577 + 10 max-button parity / API shape tests
// 592 = 587 + 5 custom-title-bar API tests (API-04)
val lockedTestTotal = 592
val lockedTestSkipped = 0

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

java {
    // Ship a -sources jar so consumers get sources/docs in their IDE.
    withSourcesJar()
}

dependencies {
    // `api` for everything that shows up in the public API surface (Modifier, @Composable,
    // LocalDate in the date pickers) so consumers get it on their compile classpath.
    api(compose.desktop.common)
    api("org.jetbrains.compose.material3:material3:1.9.0")
    api(compose.animation)
    api(compose.foundation)
    api(compose.runtime)
    api(compose.ui)
    api(libs.kotlinx.datetime)
    // Internal only — not exposed in any public signature.
    implementation(libs.kotlinx.coroutines.core)
    // Internal only — not exposed in any public signature. JNA: Windows-only native window
    // management (DEP-01).
    implementation(libs.jna)
    implementation(libs.jna.platform)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("reflect"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Compose UI testing — programmatic drag/click to reproduce recompose-during-drag
    // duplication (RCMP) that pure-logic tests cannot catch.
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation(compose.desktop.currentOs)
}

tasks.test {
    useJUnitPlatform()
    // BASE-05/D-03: images are written only when this Gradle property is explicitly passed
    // (-Paero.captureDir=<path>) — an ordinary `./gradlew test` run writes nothing.
    (project.findProperty("aero.captureDir") as String?)?.let { systemProperty("aero.captureDir", it) }

    // TOOL-16: report the executed root-suite test count so it can be measured live.
    val aeroTotal = AtomicLong(-1)
    val aeroSkipped = AtomicLong(-1)
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent == null) {
                aeroTotal.set(result.testCount)
                aeroSkipped.set(result.skippedTestCount)
            }
        }
        override fun beforeTest(testDescriptor: TestDescriptor) {}
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
    })
    doLast {
        // TestFilter's public interface (this Gradle version) exposes only build-script-configured
        // includePatterns; --tests populates commandLineIncludePatterns, which is only present on
        // the internal DefaultTestFilter implementation.
        val commandLineFiltered = (filter as? org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter)
            ?.commandLineIncludePatterns
            ?.isNotEmpty() ?: false
        val filtered = commandLineFiltered || filter.includePatterns.isNotEmpty()
        val total = aeroTotal.get()
        val skipped = aeroSkipped.get()
        println(
            "AERO_TEST_COUNT total=$total skipped=$skipped expected=$lockedTestTotal " +
                "expectedSkipped=$lockedTestSkipped filtered=$filtered"
        )
        if (!filtered && (total != lockedTestTotal.toLong() || skipped != lockedTestSkipped.toLong())) {
            throw GradleException(
                "Test count guard: executed total=$total skipped=$skipped, locked total=$lockedTestTotal " +
                    "skipped=$lockedTestSkipped"
            )
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("aero-compose-ui")
                description.set(
                    "Windows 7 Aero–styled UI components for Compose Multiplatform (Desktop/JVM)"
                )
                url.set("https://github.com/Tolaseeq/aero-compose-ui")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("Tolaseeq")
                        name.set("Tolaseeq")
                    }
                }
                scm {
                    url.set("https://github.com/Tolaseeq/aero-compose-ui")
                }
            }
        }
    }
}
