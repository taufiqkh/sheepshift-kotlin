package com.quiptiq.ludumdare.ld35.entity

/**
 * Attraction of predators to prey.
 */
class PreyPredatorAttractor : Attractor {
    companion object {
        // Distance from predator to prey (^ 2 to avoid sqrt)
        val MAX_RANGE: Float = 50f*50f
        // Will trot within this distance
        val RANGE_TROT: Float = 20f*20f
        // Will sprint within this distance
        val RANGE_RUN: Float = 10f*10f
    }

    override fun getBaseSpeed(distance: Float): Float {
        if (distance <= RANGE_RUN) {
            return 36f;
        } else if (distance <= RANGE_TROT) {
            return 12f;
        } else {
            return 3f;
        }
    }

    override val maxRange: Float = MAX_RANGE

    override val attractorType: AttractorType = AttractorType.PREY_PREDATOR
}