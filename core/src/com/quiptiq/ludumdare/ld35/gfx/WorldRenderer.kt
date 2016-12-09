package com.quiptiq.ludumdare.ld35.gfx

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.quiptiq.ludumdare.LogicHandler
import com.quiptiq.ludumdare.ld35.GraphicsHandler
import com.quiptiq.ludumdare.ld35.entity.Position
import com.quiptiq.ludumdare.ld35.entity.ProjectionTranslator
import com.quiptiq.ludumdare.ld35.game.Level
import java.util.*

class WorldRenderer : ProjectionTranslator {
    companion object {
        private val SUN_MOVEMENT_SPEED = 0.005f;
        private val CAMERA_HEIGHT = 45f;

        private val SND_MIN_DELTA: Long = 5000;
        private val MIN_FRAME_LEN = 1f / GraphicsHandler.FPS_CAP;
    }

    private lateinit var worldEnvironment: Environment

    private lateinit var sunLight: DirectionalLight

    private lateinit var shadowLight: DirectionalShadowLight
    private lateinit var shadowBatch: ModelBatch

    private lateinit var modelBatch: ModelBatch
    private lateinit var worldModelInstance: IntersectableModel
    private lateinit var playerModelInstance: IntersectableModel

    private lateinit var playerCam: PerspectiveCamera

    private val instances: com.badlogic.gdx.utils.Array<ModelInstance> =
            com.badlogic.gdx.utils.Array<ModelInstance>()
    private val noshadowInstance: com.badlogic.gdx.utils.Array<ModelInstance> =
            com.badlogic.gdx.utils.Array<ModelInstance>();

    private val animations: HashMap<ModelInstance, AnimationController> = HashMap<ModelInstance,
    AnimationController>();

    private lateinit var currentLevel: Level

    var pauseLogic = true
    var levelWon = false;
    var levelLost = false;
    var firstLevel = true;

    var justStarted = true

    val sheepAmbientSnd: Sound = Gdx.audio.newSound(Gdx.files.internal("sheep_ambient.mp3"))
    private val sheepBaa1: Sound = Gdx.audio.newSound(Gdx.files.internal("sheep_real.mp3"))
    private val sheepBaa2: Sound = Gdx.audio.newSound(Gdx.files.internal("sheep_low.mp3"))
    private val sheepBaa3: Sound = Gdx.audio.newSound(Gdx.files.internal("sheep_lol.mp3"))
    val wolfHowl: Sound = Gdx.audio.newSound(Gdx.files.internal("wolf_snarl.mp3"))
    val killSound: Sound = Gdx.audio.newSound(Gdx.files.internal("death.mp3"))

    private var lastSndPlayed: Long = 0;

    var mute: Boolean = false;

    init {
    }
    fun newLevel(increaseDifficulty: Boolean) {
        levelWon = false;
        levelLost = false;
        instances.clear();

        playerModelInstance.transform.setToTranslation(0f, 0f, 0f);
        playerModelInstance.updateCollisions();

        instances.add(worldModelInstance);
        instances.add(playerModelInstance);

        if (increaseDifficulty) {
            currentLevel = Level((currentLevel.sheepCount * 1.5f).toInt(), (currentLevel.wolfCount
                    * 1.5f).toInt(), currentLevel.complexity * 1.5f);
        } else {
            firstLevel = true;
            currentLevel = Level(10, 2, 1.0f);
        }

        instances.addAll(currentLevel.getCollisionModels());
        GraphicsHandler.logicHandler.createCreatures(modelInstanceProvider);
    }

    fun create() {
        playerCam = PerspectiveCamera(75f, Gdx.graphics.getWidth().toFloat(), Gdx.graphics
                .getHeight().toFloat());
        playerCam.position.set(0f, CAMERA_HEIGHT, 0f);
        playerCam.lookAt(0f, 0f, 0f);
        playerCam.near = 1f;
        playerCam.far = 300f;
        playerCam.update();

        worldEnvironment = Environment();
        worldEnvironment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));

        sunLight = DirectionalLight().set(0.4f, 0.4f, 0.4f, -1f, -0.4f, -1f);
        worldEnvironment.add(sunLight);

        shadowLight = DirectionalShadowLight(1024, 1024, 120f, 120f, 1f, 1000f);
        shadowLight.set(0.4f, 0.4f, 0.4f, -1f, -0.4f, -1f);
        worldEnvironment.add(shadowLight);
        worldEnvironment.shadowMap = shadowLight;

        modelBatch = ModelBatch()
        shadowBatch = ModelBatch(DepthShaderProvider())

        worldModelInstance = ModelFactory.createBoxModel(100f, 0.25f, 100f, Color.FOREST);

        playerModelInstance = ModelFactory.createCustomModel(GraphicsHandler.MDL_PLR);

        val playerAnimation: AnimationController = AnimationController(playerModelInstance);
        playerAnimation.setAnimation(playerModelInstance.animations.first().id, -1);
        animations.put(playerModelInstance, playerAnimation);

        instances.add(worldModelInstance);
        instances.add(playerModelInstance);

        currentLevel = Level(10, 2, 1.0f);

        instances.addAll(currentLevel.getCollisionModels());
        // instances.add(currentLevel.getSheepPenModel());
        // instances.addAll(currentLevel.getWolfTransformModels());

        GraphicsHandler.logicHandler.createCreatures(modelInstanceProvider);
        GraphicsHandler.logicHandler.setProjectionTranslator(this);

        sheepAmbientSnd.loop();
    }

    private fun createCreatureModel(modelFile: String, position: Vector3): IntersectableModel {
        val model: IntersectableModel = ModelFactory.createCustomModel(modelFile);
        model.transform.setToTranslation(position);

        instances.add(model);

        val anim: AnimationController = AnimationController(model);
        anim.setAnimation(model.animations.first().id, -1);
        anim.update(MathUtils.random(anim.current.duration));
        animations.put(model, anim);

        return model;
    }

    val modelInstanceProvider: LogicHandler.ModelInstanceProvider =
            object: LogicHandler.ModelInstanceProvider {
                override fun createModel(e: Entity, position: Vector3): IntersectableModel {
                    if (e.getComponent(LogicHandler.Predator::class.java) != null)
                        return createCreatureModel(GraphicsHandler.MDL_WOLF, position);
                    else
                        return createCreatureModel(GraphicsHandler.MDL_SHEEP, position);
                }
            }

    private var reverseY = false;

    fun update() {
        val delta = Gdx.graphics.getDeltaTime();
        sunLight.direction.add(delta * SUN_MOVEMENT_SPEED,
                (if (reverseY) delta * SUN_MOVEMENT_SPEED else delta * -SUN_MOVEMENT_SPEED),
                delta * SUN_MOVEMENT_SPEED);

        if (sunLight.direction.y < -1f)
            reverseY = true;
        else if (sunLight.direction.y > 0f)
            reverseY = false;

        if (sunLight.direction.z > 1f)
            sunLight.direction.z = -1f;

        if (sunLight.direction.x > 1f)
            sunLight.direction.x = -1f;

        shadowLight.setDirection(sunLight.direction);

        val logicHandler: LogicHandler = GraphicsHandler.logicHandler;
        for (entity in logicHandler.getStaleModelEntities()) {
            val position: Vector3 = entity.getComponent(Position::class.java).position;
            val oldModel: IntersectableModel = entity
                    .getComponent(LogicHandler.ModelComponent::class.java)
                    .model
            instances.removeValue(oldModel, true);
            val newModel: IntersectableModel = modelInstanceProvider.createModel(entity, position);
            entity.remove(LogicHandler.ModelComponent::class.java)
            entity.add(LogicHandler.ModelComponent(newModel));

            wolfHowl.play();
            currentLevel.transformCount++;
        }

        for (a in animations.values)
            a.update(delta);

        playerModelInstance.transform.setTranslation(logicHandler.getPlayerPosn());
        playerModelInstance.updateCollisions();

        worldModelInstance.transform.setTranslation(logicHandler.getPlayerPosn());

        playerCam.position.set(logicHandler.getPlayerPosn().x, CAMERA_HEIGHT,
                logicHandler.getPlayerPosn().z);
        playerCam.update();

        if (currentLevel.checkWin(logicHandler.getNumPenned())) {
            pauseLogic = true;
            levelWon = true;
        } else if (currentLevel.checkLose(logicHandler.getNumDead() + currentLevel.transformCount)) {
            pauseLogic = true;
            levelLost = true;
        }

        if (!mute && System.currentTimeMillis() - lastSndPlayed > SND_MIN_DELTA) {
            lastSndPlayed = System.currentTimeMillis();

            when (MathUtils.random(4)) {
                0 -> sheepBaa1.play()
                1 -> sheepBaa2.play()
                2 -> sheepBaa3.play()
            }
        }
    }

    private var timeSinceLastRender = 0f;

    @SuppressWarnings("deprecation")
    fun render() {
        timeSinceLastRender += Gdx.graphics.getDeltaTime();
        if (timeSinceLastRender < MIN_FRAME_LEN)
            return

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        shadowLight.begin(playerCam.position.cpy().add(0f, -60f, 0f), playerCam.direction)
        shadowBatch.begin(shadowLight.getCamera());

        shadowBatch.render(instances);

        shadowBatch.end();
        shadowLight.end();

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(playerCam);
        modelBatch.render(instances, worldEnvironment);
        modelBatch.render(noshadowInstance, worldEnvironment);
        modelBatch.end();

        timeSinceLastRender = 0f
    }

    fun dispose() {
        modelBatch.dispose();
    }

    fun getCurrentLevel(): Level {
        return currentLevel;
    }

    fun getPlayerModel(): IntersectableModel {
        return playerModelInstance;
    }

    fun getAnimController(model: ModelInstance): AnimationController {
        return animations.get(model)!!
    }

    override fun unproject(x: Int, y: Int): Vector3 {
        val worldPosition: Vector3 = playerCam.unproject(Vector3(x.toFloat(), y.toFloat(), 0f));
        worldPosition.y = 0f
        return worldPosition
    }

}
