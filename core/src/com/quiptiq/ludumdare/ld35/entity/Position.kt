package com.quiptiq.ludumdare.ld35.entity

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3

/**
 * Object rotation
 */
data class Position(val position: Vector3, val rotation: Vector3) : Component
