package com.quiptiq.ludumdare.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.quiptiq.ludumdare.MyGdxGame;
import com.quiptiq.ludumdare.ld35.GraphicsHandler;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1024;
		config.height = 768;
		new LwjglApplication(GraphicsHandler.Companion.getGraphicsHandler(), config);
	}
}