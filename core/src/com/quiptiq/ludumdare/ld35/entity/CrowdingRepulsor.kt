package com.quiptiq.ludumdare.ld35.entity

/**
 * Crowding repels members of a flock
 */
class CrowdingRepulsor : Attractor {
    companion object {
        // Distance to calculate crowding (^ 2 to avoid sqrt)
        private val CROWDING_RANGE: Float = 10F * 10F;
    }

    override fun getBaseSpeed(distance: Float): Float {
        return -30f * CROWDING_RANGE/(distance*distance);
    }

    override val maxRange: Float = CROWDING_RANGE

    override val attractorType: AttractorType = AttractorType.CROWDING
}