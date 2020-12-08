/*
 *	Author:      Leonard Cseres
 *	Date:        05.12.20
 *	Time:        20:43
 */


package ch.epfl.cs107.play.game.superpacman;

import ch.epfl.cs107.play.Play;
import ch.epfl.cs107.play.game.actor.Graphics;
import ch.epfl.cs107.play.game.actor.TextGraphics;
import ch.epfl.cs107.play.game.superpacman.menus.MenuItems;
import ch.epfl.cs107.play.math.Vector;
import ch.epfl.cs107.play.window.Canvas;

import java.awt.*;

public class SuperPacmanStatusGUI implements Graphics {
    private static final float DEPTH = 15000;
    private static final float TEXT_PADDING = 1.5f;
    private static final String FONT = "emulogic";
    private static final float FONT_SIZE = 1.5f;

    @Override
    public void draw(Canvas canvas) {
        float width = canvas.getScaledWidth();
        float height = canvas.getScaledHeight();

        Vector anchor = canvas.getTransform().getOrigin().sub(new Vector(width / 2, height / 2));

        // FPS counter
        if (MenuItems.isShowFps()) {
            String fps = "Fps: " + Play.getCurrentFps();
            TextGraphics fpsText =
                    new TextGraphics(fps, height / (FONT_SIZE * 35), Color.WHITE, Color.WHITE, 0.0f, false, false,
                                     anchor.add(new Vector(height / (TEXT_PADDING * 35),
                                                           height / (TEXT_PADDING * 35))));
            fpsText.setFontName(FONT);
            fpsText.setDepth(DEPTH);
            fpsText.draw(canvas);
        }

        // DEBUG MODE TEXT
        if (MenuItems.isDebugMode()) {
            String textToDisplay = "Debug Mode";
            TextGraphics debugText =
                    new TextGraphics(textToDisplay, height / (FONT_SIZE * 35), Color.ORANGE, Color.ORANGE, 0.0f, false,
                                     false,
                                     anchor.add(new Vector(width - (2 * (height / (TEXT_PADDING * 7))) -
                                                                   (height / (TEXT_PADDING * 35)),
                                                           height / (TEXT_PADDING * 35))));
            debugText.setFontName(FONT);
            debugText.setDepth(DEPTH);
            debugText.draw(canvas);
        }
    }
}
