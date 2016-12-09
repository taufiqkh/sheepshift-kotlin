package com.quiptiq.ludumdare.ld35.gfx

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.quiptiq.ludumdare.LogicHandler
import com.quiptiq.ludumdare.ld35.GraphicsHandler
import com.quiptiq.ludumdare.ld35.entity.Prey

class UIRenderer {
    companion object {
        private val DEBUG_DRAW_COLLISIONS = false;
        private val DEBUG_DRAW_ENTITYINFO = false;
        private val scale = 5
        private val offset = 100
    }

    private val batch: SpriteBatch
    private val font: BitmapFont

    private val sHead: Texture
    private val wHead: Texture

    private val layout: GlyphLayout = GlyphLayout()

    private val shapes: ShapeRenderer

    private val instructions = "Herd your sheep back into their pen " +
    "while avoiding the pesky wolves. Chase them away to " +
    "keep your sheep alive.";

    init {
        batch = SpriteBatch();
        shapes = ShapeRenderer();

        font = BitmapFont(Gdx.files.internal("testFont.fnt"));
        font.setColor(Color.WHITE);

        sHead = Texture(Gdx.files.internal("sheepHead.png"));
        wHead = Texture(Gdx.files.internal("wolfHead.png"));
    }

    fun render() {
        val w: WorldRenderer = GraphicsHandler.graphicsHandler.getWorldRenderer();
        val l: LogicHandler = GraphicsHandler.logicHandler;
        val e: Engine = w.getCurrentLevel().getEngine();

        val screenWidth = Gdx.graphics.getWidth();
        val screenHeight = Gdx.graphics.getHeight();

        val sheepCount = w.getCurrentLevel().sheepCount;
        var wolfCount = 0
        var hiddenSheepCount = 0;

        if (DEBUG_DRAW_COLLISIONS) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);

            shapes.setColor(0f, 0f, 1f, 0.5f);
            for (m: IntersectableModel in GraphicsHandler.graphicsHandler.getWorldRenderer()
                    .getCurrentLevel()
                    .getCollisionModels()) {
                val r: Rectangle = m.collision.getBoundingRectangle();
                shapes.rect(r.x * scale + offset * scale, 280 - (r.y * scale - offset), r.width * scale, r.height * scale);
            }

            shapes.setColor(1f, 1f, 1f, 0.5f);
            for (ent in e.getEntities()) {
                if (ent.getComponent(LogicHandler.ModelComponent::class.java) != null) {
                    val r: Rectangle  = ent.getComponent(LogicHandler.ModelComponent::class.java)
                    .model.collision
                    .getBoundingRectangle();
                    shapes.rect(r.x * scale + offset * scale, 280 - (r.y * scale - offset), r.width * scale, r.height * scale);
                }
            }

            val r: Rectangle = GraphicsHandler.graphicsHandler.getWorldRenderer().getPlayerModel()
                    .collision.getBoundingRectangle();
            shapes.setColor(1f, 0f, 0f, 0.5f);
            shapes.rect(r.x * scale + offset * scale, 280 - (r.y * scale - offset), r.width * scale, r.height * scale);

            shapes.end();
        }

        for (ent in e.getEntities()) {
            if (ent.getComponent(LogicHandler.PredatorHidden::class.java) != null) {
                if (!ent.getComponent(Prey::class.java).isDead)
                        hiddenSheepCount++;
            }
                if (ent.getComponent(LogicHandler.Predator::class.java) != null)
                        wolfCount++;
        }

        if (DEBUG_DRAW_ENTITYINFO) {
            batch.begin();
            font.draw(batch, "Sheep Count: " + sheepCount, 20f, 140f);
            font.draw(batch, "Hidden Wolf Count: " + hiddenSheepCount, 20f, 100f);
            font.draw(batch, "Wolf Count: " + wolfCount, 20f, 60f);
            font.draw(batch, "Pen Count: " + GraphicsHandler.logicHandler.getNumPenned(),
                    20f, 180f);
            font.draw(batch, "Dead Count: " + GraphicsHandler.logicHandler.getNumDead(),
                    20f, 220f);
            batch.end();
        }

        if (w.justStarted) {
            batch.begin();
            font.setColor(Color.WHITE);

            font.getData().setScale(4f, 3f);
            layout.setText(font, "Sheep Shift");
            font.draw(batch, "Sheep Shift", screenWidth / 2 - layout.width / 2, screenHeight / 2
                    + 200f);

            font.getData().setScale(1.5f, 1f);
            font.draw(batch, instructions, screenWidth / 4f, screenHeight / 2f, screenWidth / 2f,
                    1, true);

            font.getData().setScale(2.5f, 1.5f);
            layout.setText(font, "Click anywhere to begin");
            font.draw(batch, "Click anywhere to begin", screenWidth / 2 - layout.width / 2,
                    screenHeight / 2 - 200f);
            batch.end();

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                w.justStarted = false;
                w.pauseLogic = false;
            }
        }

        if (w.levelWon) {
            batch.begin();
            font.setColor(Color.GREEN);
            font.getData().setScale(4f, 3f);
            layout.setText(font, "Level Completed!");
            font.draw(batch, "Level Completed!", screenWidth / 2 - layout.width / 2, screenHeight
                    / 2 + 200f);

            font.getData().setScale(1.5f, 1f);
            layout.setText(font, "Sheep Saved: " + GraphicsHandler.logicHandler.getNumPenned()
                    + " / " + w.getCurrentLevel().sheepToWin);
            font.draw(batch, "Sheep Saved: " + GraphicsHandler.logicHandler.getNumPenned() + " / " +
                    "" + w.getCurrentLevel().sheepToWin,
                    screenWidth / 2 - layout.width / 2, screenHeight / 2f);

            font.setColor(Color.RED);
            layout.setText(font, "Sheep Lost: " + GraphicsHandler.logicHandler.getNumDead() + "" +
                    " / " + w.getCurrentLevel().deadToLose);
            font.draw(batch, "Sheep Lost: " + GraphicsHandler.logicHandler.getNumDead() + " / " +
                    w.getCurrentLevel().deadToLose,
                    screenWidth / 2 - layout.width / 2, screenHeight / 2 - 40f);

            font.setColor(Color.WHITE);
            font.getData().setScale(2.5f, 1.5f);
            layout.setText(font, "Click anywhere to go to the next level");
            font.draw(batch, "Click anywhere to go to the next level", screenWidth / 2 - layout
                    .width / 2, screenHeight / 2 - 150f);

            if (w.firstLevel) {
                font.setColor(Color.ORANGE);
                font.getData().setScale(1.5f);
                layout.setText(font, "beware the wolves in sheep's clothing");
                font.draw(batch, "beware the wolves in sheep's clothing", screenWidth / 2 -
                        layout.width / 2, 70f);
            }
            batch.end();

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                w.newLevel(true);
                w.pauseLogic = false;

                if (w.firstLevel)
                    w.firstLevel = false;
            }
        }

        if (w.levelLost) {
            batch.begin();
            font.setColor(Color.RED);
            font.getData().setScale(4f, 3f);
            layout.setText(font, "Level Lost!");
            font.draw(batch, "Level Lost!", screenWidth / 2 - layout.width / 2, screenHeight / 2
                    + 200f);

            font.getData().setScale(1.5f, 1f);
            layout.setText(font, "Sheep Saved: " + GraphicsHandler.logicHandler.getNumPenned()
                    + " / " + w.getCurrentLevel().sheepToWin);
            font.draw(batch, "Sheep Saved: " + GraphicsHandler.logicHandler.getNumPenned() + " / " +
                    "" + w.getCurrentLevel().sheepToWin,
                    screenWidth / 2 - layout.width / 2, screenHeight / 2f);

            font.setColor(Color.RED);
            layout.setText(font, "Sheep Lost: " + GraphicsHandler.logicHandler.getNumDead() + " /" +
                    " " + (w.getCurrentLevel().deadToLose - w.getCurrentLevel().transformCount));
            font.draw(batch, "Sheep Lost: " + GraphicsHandler.logicHandler.getNumDead() + " / "
                    + (w.getCurrentLevel().deadToLose - w.getCurrentLevel().transformCount),
                    screenWidth / 2 - layout.width / 2, screenHeight / 2 - 40f);

            font.setColor(Color.WHITE);
            font.getData().setScale(2.5f, 1.5f);
            layout.setText(font, "Click anywhere to start again");
            font.draw(batch, "Click anywhere to start again", screenWidth / 2 - layout.width / 2,
                    screenHeight / 2 - 200f);
            batch.end();

            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                w.newLevel(false);
                w.justStarted = true;
            }
        }

        if (!w.pauseLogic) {
            batch.begin();
            batch.draw(sHead, 20f, 70f, 64f, 64f);
            batch.draw(wHead, 20f, 10f, 64f, 64f);

            font.setColor(Color.WHITE);
            font.getData().setScale(1.5f, 1.5f);
            font.draw(batch, "" + (sheepCount - GraphicsHandler.logicHandler.getNumDead() -
                    w.getCurrentLevel().transformCount), 90f, 120f);
            font.draw(batch, "" + wolfCount, 90f, 60f);

            font.setColor(Color.ORANGE);
            font.getData().setScale(1f);
            layout.setText(font, "Sheep Needed: " + (w.getCurrentLevel().sheepToWin -
                    GraphicsHandler.logicHandler.getNumPenned()));
            font.draw(batch, "Sheep Needed: " + (w.getCurrentLevel().sheepToWin - GraphicsHandler
                    .logicHandler.getNumPenned()),
                    screenWidth - layout.width - 20f, 50f);

            font.setColor(Color.GRAY);
            font.getData().setScale(1f);
            font.draw(batch, "Esc to Restart", screenWidth - 120f, screenHeight - 20f);
            batch.end();

            if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
                w.newLevel(false);
                w.justStarted = true;
                w.pauseLogic = true;
            }
        }

        batch.begin();
        font.setColor(Color.GRAY);
        font.getData().setScale(1f);
        font.draw(batch, "M to Mute", screenWidth - 105f, screenHeight - 60f);
        batch.end();

        if (Gdx.input.isKeyPressed(Input.Keys.M)) {
            w.mute = !w.mute;

            if (w.mute)
                w.sheepAmbientSnd.pause();
            else
                w.sheepAmbientSnd.play();
        }
    }

    fun dispose() {
        batch.dispose();
        font.dispose();
    }
}
