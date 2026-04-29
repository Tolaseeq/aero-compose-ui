package com.mordred.aero.icons.`internal`

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import com.mordred.aero.icons.AeroIcons
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val AeroIcons.X: ImageVector
    get() {
        if (_X != null) {
            return _X!!
        }
        _X = ImageVector.Builder(
            name = "X",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(200f, 56f)
                lineTo(56f, 200f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(200f, 200f)
                lineTo(56f, 56f)
            }
        }.build()

        return _X!!
    }

@Suppress("ObjectPropertyName")
private var _X: ImageVector? = null
