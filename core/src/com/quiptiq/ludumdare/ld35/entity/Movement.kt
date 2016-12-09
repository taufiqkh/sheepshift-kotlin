package com.quiptiq.ludumdare.ld35.entity

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3

/**
 * Contains velocity and rotation data
 */
data class Movement(val velocity: Vector3, val rotation: Vector3) : Component
