import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
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

// Forward -Paero.scheme=<name> to the run task as a system property, so a review pass can open a
// specific theme directly (see initialScheme() in Main.kt) instead of clicking the theme switcher.
// withType(...).configureEach is lazy: the Compose Desktop plugin registers `run` after this
// script is evaluated, so tasks.named("run") would fail with "Task with name 'run' not found".
tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        (project.findProperty("aero.scheme") as String?)?.let { systemProperty("aero.scheme", it) }
    }
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
