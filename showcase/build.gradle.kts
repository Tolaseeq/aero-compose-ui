import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":library"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(libs.kotlinx.datetime)
}

// Forward -Paero.scheme=<name> (and aero.section / aero.page / aero.capture) to the run task as
// system properties, so a review or capture pass can open a specific theme/section/page directly
// (see initialScheme() / initialSection() / initialPage() / captureMode() in Main.kt) instead of
// clicking the theme switcher or scrolling by hand. withType(...).configureEach is lazy: the
// Compose Desktop plugin registers `run` after this script is evaluated, so tasks.named("run")
// would fail with "Task with name 'run' not found".
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
        (project.findProperty("aero.section") as String?)?.let { systemProperty("aero.section", it) }
        (project.findProperty("aero.page") as String?)?.let { systemProperty("aero.page", it) }
        (project.findProperty("aero.capture") as String?)?.let { systemProperty("aero.capture", it) }
    }
}

// Compose Hot Reload's hotRun/hotRunAsync/hotDev/hotDevAsync tasks are org.jetbrains.compose.reload
// .gradle.ComposeHotRun, a JavaExec subtype registered by the compose-hot-reload plugin rather than
// the `run` task above, so they need their own sibling forwarding block for the same aero.* launch
// parameters (see initialScheme()/initialSection()/initialPage()/captureMode() in Main.kt).
tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>().configureEach {
    (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    (project.findProperty("aero.section") as String?)?.let { systemProperty("aero.section", it) }
    (project.findProperty("aero.page") as String?)?.let { systemProperty("aero.page", it) }
    (project.findProperty("aero.capture") as String?)?.let { systemProperty("aero.capture", it) }
}

compose.desktop {
    application {
        mainClass = "com.mordred.showcase.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Deb)
            packageName = "aero-compose-ui-showcase"
            packageVersion = "0.1.0"
        }
    }
}
