package com.quiptiq.ludumdare.ld35.entity

import com.badlogic.ashley.core.Component

/**
 * Contains basic prey states
 */
class Prey : Component {
    var isPenned = false
    var isDead: Boolean = false
        private set

    fun kill() {
        this.isDead = true
    }
}