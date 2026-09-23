package com.mordred.aero.capture

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import com.mordred.aero.theme.AeroColorScheme
import java.io.File
import javax.imageio.ImageIO

/**
 * Opt-in PNG writer shared by every BASE-05/D-07 capture test (D-03).
 *
 * [dir] is `null` unless the `aero.captureDir` system property is explicitly passed (forwarded
 * from the `-Paero.captureDir=<path>` Gradle property by `library/build.gradle.kts`'s
 * `tasks.test` block) — so an ordinary `./gradlew test` run never writes into the working tree.
 */
internal object UiCapture {

    /** Root directory for this run's images, or `null` when opt-in was not requested (D-03). */
    val dir: File? = System.getProperty("aero.captureDir")?.let(::File)

    /**
     * Writes [image] to `<dir>/<component>/<theme>/<state>.png`, creating parent directories as
     * needed. No-op when [dir] is `null` — the default, non-opt-in path (D-03).
     */
    fun write(component: String, theme: String, state: String, image: ImageBitmap) {
        val root = dir ?: return
        val target = File(File(root, component), theme)
        target.mkdirs()
        val file = File(target, "$state.png")
        ImageIO.write(image.toAwtImage(), "png", file)
    }

    /** The three built-in color schemes, in the order every capture test iterates them. */
    val SCHEMES: List<Pair<String, AeroColorScheme>> = listOf(
        "AeroBlue" to AeroColorScheme.AeroBlue,
        "AeroDark" to AeroColorScheme.AeroDark,
        "Classic" to AeroColorScheme.Classic,
    )
}
