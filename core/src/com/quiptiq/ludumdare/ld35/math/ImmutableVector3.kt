package com.quiptiq.ludumdare.ld35.math

import com.badlogic.gdx.math.Vector
import com.badlogic.gdx.math.Vector3

/**
 * Immutable vector3 representation, prevents accidental mutation of a vec3
 */
data class ImmutableVector3(private val vector3: Vector3) {

    constructor(x: Float, y: Float, z: Float) : this(Vector3(x, y, z))

    val x: Float
        get() = vector3.x

    val y: Float
        get() = vector3.y

    val z: Float
        get() = vector3.z

    fun copy(): Vector3 {
        return Vector3(vector3);
    }
}
