package com.quiptiq.ludumdare.ld35.entity

/**
 * Type of attractor, defines how entities are attracted
 */
enum class AttractorType {
    CROWDING,
    COHESION,
    HIDDEN_PREDATOR,
    // Prey-to-predator
    PREY_PREDATOR,
    // Predator-to-prey
    PREDATOR_PREY,
    PLAYER,
    FIXED_OBSTACLE
}