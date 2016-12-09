package com.quiptiq.ludumdare.ld35.math

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.quiptiq.ludumdare.ld35.entity.Position

/**
 * Encapsulates movement calculation code
 */
class MovementCalculator(private val defaultRotation: ImmutableVector3) {
    companion object {
        private val UP: ImmutableVector3 = ImmutableVector3(0f, 1f, 0f);
    }

    fun calculateNewDirection(rotation: Vector3, movementDirection: Vector3): Vector3 {
        if (rotation.isZero()) {
            if (movementDirection.isZero()) {
                return defaultRotation.copy();
            }
            return movementDirection;
        } else if (movementDirection.isZero()) {
            return rotation;
        }
        var average: Vector3 = Vector3(rotation.add(movementDirection)).nor();
        if (average.isZero()) {
            average = rotation.rotateRad(UP.copy(), MathUtils.PI);
        }
        return average;
    }

    // Calculates a new vector from the origin away from the target
    fun awayFrom(origin: Position, target: Position): Vector3 {
        return Vector3(origin.position).sub(target.position);
    }

    // Calculates a new vector from the origin to the target
    fun towards(origin: Position, target: Position): Vector3 {
        return Vector3(target.position).sub(origin.position);
    }
}