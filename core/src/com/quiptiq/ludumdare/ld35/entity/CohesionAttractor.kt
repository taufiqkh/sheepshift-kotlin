package com.quiptiq.ludumdare.ld35.entity

/**
 * Cohesion
 */
class CohesionAttractor : Attractor {
    companion object {
        // Distance to calculate cohesion (^ 2 to avoid sqrt)
        private val COHESION_RANGE: Float = (30 * 30).toFloat()
    }

    override fun getBaseSpeed(distance: Float): Float {
        return 60F
    }

    override val maxRange: Float = COHESION_RANGE

    override val attractorType: AttractorType = AttractorType.COHESION
}