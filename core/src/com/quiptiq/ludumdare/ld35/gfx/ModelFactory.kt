package com.quiptiq.ludumdare.ld35.gfx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.quiptiq.ludumdare.ld35.GraphicsHandler

object ModelFactory {
    private val modelBuilder: ModelBuilder = ModelBuilder();

    fun createBoxModel(sizeX: Float, sizeY: Float, sizeZ: Float, c: Color): IntersectableModel {
        val model: Model = modelBuilder.createBox(sizeX, sizeY, sizeZ, Material(ColorAttribute
                .createDiffuse(c)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())

        return IntersectableModel("box", model);
    }

    fun createSphereModel(sizeX: Float, sizeY: Float, sizeZ: Float, c: Color, divisions: Int):
            IntersectableModel {
        val model: Model = modelBuilder.createSphere(sizeX, sizeY, sizeZ, divisions, divisions,
            Material(ColorAttribute.createDiffuse(c)), (VertexAttributes.Usage.Position or
                VertexAttributes.Usage.Normal).toLong())

        return IntersectableModel("sphere", model);
    }

    fun createCustomModel(modelFile: String): IntersectableModel {
        val model: Model = GraphicsHandler.graphicsHandler.getAssets().get(modelFile,
                Model::class.java);

        return IntersectableModel(modelFile, model);
    }

    fun intersectsWith(boundingBox1: BoundingBox, boundingBox2: BoundingBox): Boolean {
        val otherMin: Vector3 = boundingBox1.min;
        val otherMax: Vector3 = boundingBox1.max;
        val min: Vector3 = boundingBox2.min;
        val max: Vector3 = boundingBox2.max;

        return (min.x < otherMax.x) && (max.x > otherMin.x)
                && (min.y < otherMax.y) && (max.y > otherMin.y)
                && (min.z < otherMax.z) && (max.z > otherMin.z);
    }

}