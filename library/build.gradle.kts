plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
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
    api(compose.material3)
    api(compose.animation)
    api(compose.foundation)
    api(compose.runtime)
    api(compose.ui)
    api(libs.kotlinx.datetime)
    // Internal only — not exposed in any public signature.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("reflect"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
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
