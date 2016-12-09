package com.quiptiq.ludumdare.ld35.entity

/**
 * Fixed object that repulses entities
 */
class FixedObjectRepulsor : Attractor {
    companion object {
        val RANGE: Float = 15F*15F;
    }

    override fun getBaseSpeed(distance: Float): Float {
        if (distance == 0F) {
            return Float.MIN_VALUE;
        }
        return -60f * RANGE / (distance * distance);
    }

    override val maxRange: Float = RANGE

    override val attractorType: AttractorType = AttractorType.FIXED_OBSTACLE
}