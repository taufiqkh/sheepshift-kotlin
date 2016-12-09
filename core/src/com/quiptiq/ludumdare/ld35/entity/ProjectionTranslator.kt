package com.quiptiq.ludumdare.ld35.entity

import com.badlogic.gdx.math.Vector3

/**
 * Translates screen x y to world x y
 */
interface ProjectionTranslator {
    fun unproject(x: Int, y: Int): Vector3
}
