package com.quiptiq.ludumdare.ld35

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g3d.Model
import com.quiptiq.ludumdare.LogicHandler
import com.quiptiq.ludumdare.ld35.gfx.UIRenderer
import com.quiptiq.ludumdare.ld35.gfx.WorldRenderer

class GraphicsHandler : ApplicationAdapter() {
    companion object {
        val FPS_CAP: Float = 90f

        val logicHandler: LogicHandler = LogicHandler()
        val graphicsHandler: GraphicsHandler = GraphicsHandler()

        val MDL_PLR: String = "playerTest.g3dj";
        val MDL_SHEEP: String = "sheepOriginal.g3dj";
        val MDL_WOLF = "wolfOriginal.g3dj"
        val MDL_FENCE1 = "woodFence.g3dj"
        val MDL_TREE1 = "treeOne.g3dj"
        val MDL_TREE2 = "treeTwo.g3dj"
        val MDL_GRASS = "grassTest.g3dj"
        val MDL_ROCK1 = "rockOne.g3dj"

    }
        private lateinit var worldRenderer: WorldRenderer
        private lateinit var uiRenderer: UIRenderer

        private lateinit var assets: AssetManager

        private var loadingAssets: Boolean = true;

    override fun create() {
        assets = AssetManager();
        assets.load(MDL_PLR, Model::class.java)
        assets.load(MDL_SHEEP, Model::class.java)
        assets.load(MDL_WOLF, Model::class.java)
        assets.load(MDL_FENCE1, Model::class.java)
        assets.load(MDL_TREE1, Model::class.java)
        assets.load(MDL_TREE2, Model::class.java)
        assets.load(MDL_GRASS, Model::class.java)
        assets.load(MDL_ROCK1, Model::class.java)

        worldRenderer = WorldRenderer()
        uiRenderer = UIRenderer()
    }

    fun doneLoading() {
        loadingAssets = false;

        worldRenderer.create();
    }

    override fun render() {
        if (loadingAssets) {
            if (assets.update())
                doneLoading();
            return;
        }

        if (!worldRenderer.pauseLogic)
            GraphicsHandler.logicHandler.update();

        worldRenderer.update();
        worldRenderer.render();

        uiRenderer.render();
    }

    override fun dispose() {
        worldRenderer.dispose();
        uiRenderer.dispose();
        assets.dispose();
    }

    fun getAssets(): AssetManager {
        return assets;
    }

    fun getWorldRenderer(): WorldRenderer {
        return worldRenderer;
    }

}
