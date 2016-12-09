package com.quiptiq.ludumdare.ld35.entity

/**
 * There's something not quite right about that one...
 */
class HiddenPredatorRepulsor : Attractor {
    companion object {
        // Distance to calculate (^ 2 to avoid sqrt)
        private val RANGE: Float = 10f * 10f;
    }

    override fun getBaseSpeed(distance: Float): Float {
        return -120f * RANGE /(distance*distance);
    }

    override fun getMaxRange(): Float {
        return RANGE;
    }

    override fun getAttractorType(): AttractorType {
        return AttractorType.HIDDEN_PREDATOR;
    }
}