package com.quiptiq.ludumdare.ld35.entity

/**
 * Repulsion of prey from predators.
 */
class PredatorPreyRepulsor : Attractor {
    companion object {
        // Distance from predator to prey (^ 2 to avoid sqrt)
        private val RANGE: Float = 30f*30f
    }

    override fun getBaseSpeed(distance: Float): Float {
        return -30f
    }

    override val maxRange: Float = RANGE

    override val attractorType: AttractorType = AttractorType.PREY_PREDATOR
}