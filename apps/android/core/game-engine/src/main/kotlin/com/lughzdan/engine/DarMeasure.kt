package com.lughzdan.engine

import kotlin.math.abs
import kotlin.math.tan

object DarMeasure {
    fun shadow(height: Double, elevation: Int): Double {
        require(height.isFinite() && height > 0 && elevation in 20..70)
        return height / tan(Math.toRadians(elevation.toDouble()))
    }
    fun matches(cm: Double, height: Double, elevation: Int) =
        cm.isFinite() && abs(cm - shadow(height, elevation)) <= 0.5
}
