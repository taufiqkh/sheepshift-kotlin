package com.quiptiq.ludumdare.ld35.entity

import com.badlogic.ashley.core.Component

/**
 * Attracts flock entities. Negative attraction acts as repellant.
 */
interface Attractor : Component {
    fun getBaseSpeed(distance: Float): Float

    val maxRange: Float

    val attractorType: AttractorType
}