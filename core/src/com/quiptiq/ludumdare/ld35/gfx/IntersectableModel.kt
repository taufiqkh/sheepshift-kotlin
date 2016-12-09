package com.quiptiq.ludumdare.ld35.gfx

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.BoundingBox

class IntersectableModel(private val name: String, model: Model): ModelInstance(model) {
    // name for debugging

    private var bounds: BoundingBox = BoundingBox()

    val collision: Polygon

    init {
        bounds = calculateBoundingBox(bounds)
        bounds.ext(-(bounds.width * 0.2f), 0f, -(bounds.depth * 0.2f))

        collision = Polygon(floatArrayOf(
                bounds.min.x, bounds.min.z,
                bounds.max.x, bounds.min.z,
                bounds.max.x, bounds.max.z,
                bounds.min.x, bounds.max.z))

        collision.setOrigin(bounds.centerX, bounds.centerZ)
    }

    fun intersects(m: IntersectableModel): Boolean {
        updateCollisions()
        m.updateCollisions()

        return collision.boundingRectangle.overlaps(m.collision.boundingRectangle)
    }

    fun contains(m: IntersectableModel): Boolean {
        updateCollisions()
        m.updateCollisions()

        return collision.boundingRectangle.contains(m.collision.boundingRectangle)
    }

    fun updateCollisions() {
        collision.setPosition(transform.getTranslation(Vector3()).x, transform.getTranslation(Vector3()).z)
        collision.rotation = transform.getRotation(Quaternion()).getAngleAround(Vector3.Y)
        collision.setScale(transform.scaleX, transform.scaleZ)
    }

    fun lineIntersectsCollision(p1: Vector2, p2: Vector2): Boolean {
        updateCollisions()

        val r: Rectangle = collision.boundingRectangle

        return lineIntersectsLine(p1, p2, Vector2(r.x, r.y), Vector2(r.x + r.width, r.y)) ||
        lineIntersectsLine(p1, p2, Vector2(r.x + r.width, r.y), Vector2(r.x + r.width, r.y + r.height)) ||
        lineIntersectsLine(p1, p2, Vector2(r.x + r.width, r.y + r.height), Vector2(r.x, r.y + r.height)) ||
        lineIntersectsLine(p1, p2, Vector2(r.x, r.y + r.height), Vector2(r.x, r.y)) ||
        (r.contains(p1) && r.contains(p2))
    }

    companion object {
        private fun lineIntersectsLine(l1p1: Vector2, l1p2: Vector2, l2p1: Vector2, l2p2: Vector2):
                Boolean {
            var q = (l1p1.y - l2p1.y) * (l2p2.x - l2p1.x) - (l1p1.x - l2p1.x) * (l2p2.y - l2p1.y)
            val d = (l1p2.x - l1p1.x) * (l2p2.y - l2p1.y) - (l1p2.y - l1p1.y) * (l2p2.x - l2p1.x)

            if (d == 0f)
                return false

            val r = q / d

            q = (l1p1.y - l2p1.y) * (l1p2.x - l1p1.x) - (l1p1.x - l2p1.x) * (l1p2.y - l1p1.y)
            val s = q / d

            if (r < 0 || r > 1 || s < 0 || s > 1)
                return false

            return true
        }
    }
}