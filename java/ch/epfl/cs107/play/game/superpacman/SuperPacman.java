/*
 *	Author:      Leonard Cseres
 *	Date:        25.11.20
 *	Time:        16:41
 */


package ch.epfl.cs107.play.game.superpacman;

import ch.epfl.cs107.play.game.areagame.Area;
import ch.epfl.cs107.play.game.rpg.RPG;
import ch.epfl.cs107.play.game.rpg.actor.Player;
import ch.epfl.cs107.play.game.superpacman.actor.Arcade;
import ch.epfl.cs107.play.game.superpacman.actor.SuperPacmanPlayer;
import ch.epfl.cs107.play.game.superpacman.area.levels.Level0;
import ch.epfl.cs107.play.game.superpacman.area.levels.Level1;
import ch.epfl.cs107.play.game.superpacman.area.levels.Level2;
import ch.epfl.cs107.play.io.FileSystem;
import ch.epfl.cs107.play.window.Window;

public class SuperPacman extends RPG {
    public static float CAMERA_SCALE_FACTOR = 170.f;
    private static Arcade arcade;
    private final String[] areas = {"superpacman/level0", "superpacman/level1", "superpacman/level2"};
    private SuperPacmanPlayer player;
    private int areaIndex;
    private float progress = 0.0f;
    private boolean start = false;

    /* ----------------------------------- ACCESSORS ----------------------------------- */
    // TODO: Temporary fix, find better solution
    public static Arcade getArcade() {
        return arcade;
    }

    @Override
    public String getTitle() {
        return "Super Pac-mac";
    }


    /**
     * Method to add Areas to the AreaGame
     */
    private void createAreas() {
        addArea(new Level0());
        addArea(new Level1());
        addArea(new Level2());
    }

    @Override
    protected void initPlayer(Player player) {
        super.initPlayer(player);
    }

    @Override
    public boolean begin(Window window, FileSystem fileSystem) {

        if (super.begin(window, fileSystem)) {
            createAreas();
            areaIndex = 0;
            Area area = setCurrentArea(areas[areaIndex], true);
            player = new SuperPacmanPlayer(area, Level0.PLAYER_SPAWN_POSITION);
            initPlayer(player);
            arcade = new Arcade();
            area.registerActor(arcade);
            return true;
        }
        return false;
    }

    @Override
    public void update(float deltaTime) {
        // TODO: Temporary fix, find better solution
        if (!start) {
            progress += 0.005f;
            if (progress <= 1) {
                arcade.setAlpha(BezierBlend(progress));
            } else {
                progress = 0;
                start = true;
            }
        } else {
            progress += 0.01f;
        }

        if (CAMERA_SCALE_FACTOR > 25.0f && start && progress <= 1) {
            float newProgress = BezierBlend(progress);
            CAMERA_SCALE_FACTOR = 25 + (145 * (1 - newProgress));
        } else if (start) {
            player.setCanPlayerMove(true);
        }


        super.update(deltaTime);
    }

    private float BezierBlend(float x) {
        return x < 0.5 ? 8 * x * x * x * x : (float) (1 - Math.pow(-2 * x + 2, 4) / 2);
    }

    @Override
    public void end() {
    }


}
