package com.quiptiq.ludumdare.ld35.game

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array as GdxArray
import com.quiptiq.ludumdare.LogicHandler
import com.quiptiq.ludumdare.ld35.GraphicsHandler
import com.quiptiq.ludumdare.ld35.entity.FixedObjectRepulsor
import com.quiptiq.ludumdare.ld35.entity.Movement
import com.quiptiq.ludumdare.ld35.entity.Position
import com.quiptiq.ludumdare.ld35.gfx.IntersectableModel
import com.quiptiq.ludumdare.ld35.gfx.ModelFactory

class Level(sheepCount: Int, wolfCount: Int, complexity: Float) {
    companion object {
        val SHEEP_PEN_W: Int = 3
        val SHEEP_PEN_H: Int = 3
    }

    var sheepCount: Int
    var wolfCount: Int
    val sheepToWin: Int
    val deadToLose: Int

    var transformCount: Int = 0;

    val complexity: Float
    val hiddenSheepChance: Float

    private val collisions: GdxArray<IntersectableModel> =
            GdxArray<IntersectableModel>();

    private lateinit var sheepPenModel: IntersectableModel

    private lateinit var wolfMB: GdxArray<IntersectableModel>

    private lateinit var mapModel: IntersectableModel

    private lateinit var engine: Engine
    private lateinit var player: Entity

    init {
        this.sheepCount = sheepCount;
        this.wolfCount = 0;//wolfCount;
        this.complexity = complexity;

        this.sheepToWin = (sheepCount / (5f / complexity)).toInt();
        this.deadToLose = sheepCount - sheepToWin + 1;

        this.hiddenSheepChance = 0.7f;//complexity - 1.0f;

        this.engine = Engine();

        generateLevel(100f * complexity);
    }

    fun checkLose(sheepDead: Int): Boolean {
        if (sheepDead >= deadToLose)
            return true;

        return false;
    }

    fun checkWin(sheepInPen: Int): Boolean {
        return sheepInPen >= sheepToWin
    }

    fun generateLevel(mapSize: Float) {
        player = Entity();
        player.add(Position(Vector3(), Vector3()));
        player.add(Movement(Vector3(), Vector3()));
        player.add(LogicHandler.Mouseable());
        player.add(LogicHandler.ModelComponent(GraphicsHandler.graphicsHandler.getWorldRenderer
        ().getPlayerModel()));

        engine.addEntity(player);

        createBounds(mapSize);

        val penSize: Int = (SHEEP_PEN_W + MathUtils.random(complexity)).toInt();
        createSheepPen(mapSize, penSize, penSize);
        createWolfTransform(mapSize);

        for (i in 0 until (mapSize / (20f / complexity)).toInt()) {
            createTree(mapSize, 0.5f);
        }

        var fine: Boolean
        val testVec: Vector2 = Vector2();
        for (i in 0 until sheepCount) {
            do {
                fine = true;

                testVec.x = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);
                testVec.y = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);

                if (sheepPenModel.lineIntersectsCollision(testVec, testVec))
                    fine = false;
                for (m in collisions) {
                    if (m.lineIntersectsCollision(testVec, testVec))
                        fine = false;
                }
            } while (!fine);

            val posX = testVec.x;
            val posZ = testVec.y;

            val sheepEntity: Entity = GraphicsHandler.logicHandler.createNewPrey(
                    Vector3(posX, 0f, posZ), Vector3());

            if (MathUtils.randomBoolean(hiddenSheepChance))
                sheepEntity.add(LogicHandler.PredatorHidden());

            engine.addEntity(sheepEntity);
        }

        for (i in 0 until wolfCount) {
            do {
                fine = true;

                testVec.x = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);
                testVec.y = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);

                if (sheepPenModel.lineIntersectsCollision(testVec, testVec) && complexity < 2f)
                    fine = false;
                for (m in collisions) {
                    if (m.lineIntersectsCollision(testVec, testVec))
                        fine = false;
                }
            } while (!fine);

            val posX = testVec.x;
            val posZ = testVec.y;

            engine.addEntity(GraphicsHandler.logicHandler.createNewPredator(
                    Vector3(posX, 0f, posZ), Vector3()));
        }
    }

    private fun newCollision(x: Float, y: Float, z: Float, scale: Float, rotation: Float, model:
    String):
    IntersectableModel {
        val c: IntersectableModel = ModelFactory.createCustomModel(model);
        c.transform.setToTranslation(x, y, z).rotate(Vector3.Y, rotation).scale(scale, scale, scale);

        return c;
    }

    fun addCollisionModel(c: IntersectableModel) {
        if (collisions.contains(c, false))
            return;

        c.updateCollisions();

        collisions.add(c);
    }

    fun getCollisionModels(): GdxArray<IntersectableModel> {
        return collisions;
    }

    fun getSheepPenModel(): IntersectableModel {
        return sheepPenModel;
    }

    fun getWolfTransformModels(): GdxArray<IntersectableModel> {
        return wolfMB;
    }

    fun getPlayer(): Entity {
        return player;
    }

    fun getEngine(): Engine {
        return engine;
    }

    fun dispose() {
        collisions.clear();
        engine.removeAllEntities();
    }

    private fun createTree(mapSize: Float, weight: Float) {
        val treeType: String
        val rotateAmnt: Float
        when (MathUtils.random(2)) {
            0 -> {
                treeType = GraphicsHandler.MDL_TREE1;
                rotateAmnt = 360f * MathUtils.random() - 180f;
            }
            1 -> {
                treeType = GraphicsHandler.MDL_TREE2;
                rotateAmnt = 360f * MathUtils.random() - 180f;
            }
            2 -> {
                treeType = GraphicsHandler.MDL_ROCK1;
                rotateAmnt = MathUtils.random(3) * 90f;
            }
            else -> {
                return
            }
        }

        val treeInstance = ModelFactory.createCustomModel(treeType);
        var collisionModel: IntersectableModel? = null
        var okay: Boolean
        var penLocX: Float
        var penLocZ: Float
        do {
            okay = true;

            penLocX = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);
            penLocZ = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);

            treeInstance.transform.setToTranslation(penLocX, 0f, penLocZ);

            for (b in collisions)
            if (b.intersects(treeInstance)) {
                okay = false;
                break;
            }

            if (okay) {
                if (treeInstance.intersects(sheepPenModel))
                    okay = false;
                for (b in wolfMB)
                if (treeInstance.intersects(b))
                    okay = false;

                if (treeInstance.intersects(GraphicsHandler.graphicsHandler.getWorldRenderer()
                        .getPlayerModel()))
                    okay = false;

                collisionModel = newCollision(penLocX, 0f, penLocZ, 0.5f + MathUtils.random(1f), rotateAmnt, treeType);
                if (collisionModel != null && collisionModel.intersects(sheepPenModel)) {
                    collisionModel = null;
                    okay = false;
                }
            }
        } while (!okay)
        if (collisionModel != null) {
            addFixedEntity(collisionModel, penLocX, penLocZ);
            addCollisionModel(collisionModel);
        }
    }

    private fun createWolfTransform(mapSize: Float) {
        wolfMB = GdxArray<IntersectableModel>();

        for (i in 0 until (mapSize / 50f).toInt()) {
            val wolfTransform: IntersectableModel  = ModelFactory.createBoxModel(10f, 0.5f, 10f,
                    Color(0.8f, 0.2f, 0.2f, 1f));

            do {
                val penLocX = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);
                val penLocZ = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);

                wolfTransform.transform.setToTranslation(penLocX, 0f, penLocZ);

            } while (wolfTransform.intersects(sheepPenModel) || !mapModel.contains(wolfTransform));

            wolfMB.add(wolfTransform);
        }
    }

    private fun createSheepPen(mapSize: Float, penW: Int, penH: Int) {
        var penLocX: Float
        var penLocZ: Float

        val fenceB: BoundingBox = ModelFactory.createCustomModel(GraphicsHandler.MDL_FENCE1).model
                .calculateBoundingBox(BoundingBox());

        sheepPenModel = ModelFactory.createBoxModel(penW * fenceB.getWidth(), 0.5f, penH * fenceB.getWidth(), Color(0.5f, 0.5f, 0.5f, 1f));

        do {
            penLocX = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);
            penLocZ = ((mapSize / 2) * -1) + (MathUtils.random() * mapSize);

            sheepPenModel.transform.setTranslation(penLocX, 0f, penLocZ);
        } while(!mapModel.contains(sheepPenModel));

        val entryPoint: Int = MathUtils.random(penW - 1)
        var x = false
        var firstDir = false
        if (penLocZ < 0) {
            if (penLocX < 0) {
                if (penLocZ <= penLocX) {
                    x = true;
                    firstDir = true;
                }
            } else {
                if (penLocZ * -1 >= penLocX) {
                    x = true;
                    firstDir = true;
                } else {
                    firstDir = true;
                }
            }
        } else {
            if (penLocX < 0) {
                if (penLocZ * -1 <= penLocX) {
                    x = true;
                }
            } else {
                if (penLocZ >= penLocX) {
                    x = true;
                } else {
                    firstDir = true;
                }
            }
        }

        System.out.println(x.toString() + "," + firstDir + "," + entryPoint);
        for (i in 0 until penW) {
            if (x && i == entryPoint) {
                if (firstDir)
                    createFence(penLocX + ((i - penW / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f),
                            penLocZ - ((penH / 2f) * fenceB.getWidth()), 1.0f, 0f);
                else
                    createFence(penLocX + ((i - penW / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f),
                            penLocZ + ((penH / 2f) * fenceB.getWidth()), 1.0f, 0f);

                continue;
            }

            // North Fence
            createFence(penLocX + ((i - penW / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f),
                    penLocZ - ((penH / 2f) * fenceB.getWidth()), 1.0f, 0f);
            // South Fence
            createFence(penLocX + ((i - penW / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f),
                    penLocZ + ((penH / 2f) * fenceB.getWidth()), 1.0f, 0f);
        }

        for (i in 0 until penH) {
            if (!x && i == entryPoint) {
                if (firstDir)
                    createFence(penLocX + ((penW / 2f) * fenceB.getWidth()),
                            penLocZ + ((i - penH / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f), 1.0f, 90f);
                else
                    createFence(penLocX - ((penW / 2f) * fenceB.getWidth()),
                            penLocZ + ((i - penH / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f), 1.0f, 90f);

                continue;
            }

            // West Fence
            createFence(penLocX - ((penW / 2f) * fenceB.getWidth()),
                    penLocZ + ((i - penH / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f), 1.0f, 90f);
            // East Fence
            createFence(penLocX + ((penW / 2f) * fenceB.getWidth()),
                    penLocZ + ((i - penH / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f), 1.0f, 90f);
        }
    }

    private fun createFence(x: Float, z: Float, scale: Float, rotation: Float): IntersectableModel {
        val model: IntersectableModel = newCollision(x, 0f, z, scale, rotation, GraphicsHandler
                .MDL_FENCE1);
        addCollisionModel(model);
        addFixedEntity(model, x, z);
        return model;
    }

    private fun addFixedEntity(model: IntersectableModel, x: Float, z: Float) {
        engine.addEntity(Entity()
                .add(FixedObjectRepulsor())
                .add(LogicHandler.ModelComponent(model))
                .add(Position(Vector3(x, 0f, z), Vector3())) // rotation unused
        );
    }

    private fun createBounds(mapSize: Float) {
        mapModel = ModelFactory.createBoxModel(mapSize, 0.5f, mapSize, Color(0.2f, 0.2f, 0.8f, 1f));
        mapModel.transform.setTranslation(0f, 0f, 0f);

        val penMapSize = mapSize * 1.1f;

        val fenceB: BoundingBox = ModelFactory.createCustomModel(GraphicsHandler.MDL_FENCE1).model
                .calculateBoundingBox(BoundingBox());

        val penW = (penMapSize / fenceB.getWidth()).toInt();
        val penH = penW;

        for (i in 0 until penW) {
            // North Fence
            createFence(((i - penW / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f),
                    -((penH / 2f) * fenceB.getWidth()), 1.0f, 0f);
            // South Fence
            createFence(((i - penW / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f),
                    ((penH / 2f) * fenceB.getWidth()), 1.0f, 0f);
            // West Fence
            createFence(-((penW / 2f) * fenceB.getWidth()),
                    ((i - penH / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f), 1.0f, 90f);
            // East Fence
            createFence(((penW / 2f) * fenceB.getWidth()),
                    ((i - penH / 2f) * fenceB.getWidth()) + (fenceB.getWidth() / 2f), 1.0f, 90f);
        }
    }
}
