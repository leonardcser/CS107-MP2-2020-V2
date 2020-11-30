/*
 *	Author:      Leonard Cseres
 *	Date:        29.11.20
 *	Time:        19:20
 */


package ch.epfl.cs107.play.game.superpacman.actor;

import ch.epfl.cs107.play.game.actor.SoundAcoustics;
import ch.epfl.cs107.play.game.areagame.Area;
import ch.epfl.cs107.play.game.areagame.actor.*;
import ch.epfl.cs107.play.game.areagame.handler.AreaInteractionVisitor;
import ch.epfl.cs107.play.game.areagame.io.ResourcePath;
import ch.epfl.cs107.play.game.rpg.actor.RPGSprite;
import ch.epfl.cs107.play.game.superpacman.area.SuperPacmanAreaBehavior;
import ch.epfl.cs107.play.game.superpacman.handler.SuperPacmanInteractionVisitor;
import ch.epfl.cs107.play.math.DiscreteCoordinates;
import ch.epfl.cs107.play.math.RandomGenerator;
import ch.epfl.cs107.play.window.Audio;
import ch.epfl.cs107.play.window.Canvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public abstract class Ghost extends MovableAreaEntity implements Interactor {
    // Default attributes
    protected static final int GHOST_SCORE = 200;
    protected static final float RESET_TIME = 3;
    protected static final float EATEN_TIME = 2;
    private static final int ANIMATION_DURATION = 12;
    private static final int BACK_TO_HOME_ANIMATION_DURATION = 5;
    private static final float FRIGHTENED_TIME = 30;
    private static final Orientation DEFAULT_ORIENTATION = Orientation.RIGHT;
    private static final int NORMAL_GLOW = 0;
    private static final int FRIGHTENED_GLOW = 1;
    private static int maxSounds = 0;

    // Visuals
    private final Animation[] normalAnimation;
    private final Animation frightenedAnimation;
    private final Animation[] backToHomeAnimation;
    private final SoundAcoustics retreatingSound;
    private final Glow[] glows = new Glow[2];

    // Class management
    private final GhostInteractionHandler ghostHandler;
    private boolean reset = false;
    private boolean paused = false;
    private float pauseTime = 0;
    private float timer = 0;
    private boolean timerIsFinished = false;
    private boolean soundHasStarted = false;

    // Movement
    private Orientation currentOrientation = DEFAULT_ORIENTATION;
    private int movementDuration = ANIMATION_DURATION;

    // Ghost key attributes
    private final DiscreteCoordinates homePosition;
    private DiscreteCoordinates scatterPosition;
    private boolean chase = false;
    private boolean frightened = true;
    private float frightenedTime = FRIGHTENED_TIME;
    private boolean playerInView = false;
    private boolean isEaten = false;
    private boolean stateUpdate = false;
    private boolean hasReset = false;

    // Orientation pathing
    private Queue<Orientation> path = null;
    private DiscreteCoordinates targetPos = null;
    private DiscreteCoordinates lastPlayerPosition;

    /**
     * Constructor for Ghost
     * @param area       (Area): Owner area. Not null
     * @param position   (Coordinate): Initial position of the entity. Not null
     * @param spriteName (String): The name of the Ghost sprite
     * @param spriteSize (int): The size of the Ghost sprite
     * @param glowColor  (Glow.GlowColors): The glow color for the Ghost sprite
     */
    public Ghost(Area area, DiscreteCoordinates position, String spriteName, int spriteSize,
                 Glow.GlowColors glowColor) {
        super(area, DEFAULT_ORIENTATION, position);
        ghostHandler = new GhostInteractionHandler();
        homePosition = position;
        scatterPosition = position;

        // ANIMATIONS
        // Frighted Animation
        Sprite[] frightenedSprites = RPGSprite.extractSprites("superpacman/ghost.afraid", 2, 1, 1, this, 16, 16);
        frightenedAnimation = new Animation(ANIMATION_DURATION / 2, frightenedSprites);

        // Normal Animation
        Sprite[][] sprites = RPGSprite.extractSprites(spriteName,
                                                      2,
                                                      1,
                                                      1,
                                                      this,
                                                      spriteSize,
                                                      spriteSize,
                                                      new Orientation[]{Orientation.UP, Orientation.RIGHT,
                                                                        Orientation.DOWN, Orientation.LEFT});
        normalAnimation = Animation.createAnimations(ANIMATION_DURATION / 2, sprites);

        // BackToHome sprites
        Sprite[][] eyesSprites = RPGSprite.extractSprites("superpacman/ghost.eyes",
                                                          1,
                                                          1,
                                                          1,
                                                          this,
                                                          16,
                                                          16,
                                                          new Orientation[]{Orientation.UP, Orientation.RIGHT,
                                                                            Orientation.DOWN, Orientation.LEFT});
        backToHomeAnimation = Animation.createAnimations(0, eyesSprites);

        // GLOW
        glows[NORMAL_GLOW] = new Glow(this, sprites[0][0], glowColor, 5.0f, 0.6f);
        glows[FRIGHTENED_GLOW] = new Glow(this, sprites[0][0], Glow.GlowColors.BLUE, 5.0f, 0.6f);

        // SOUNDS
        retreatingSound =
                new SoundAcoustics(ResourcePath.getSounds("superpacman/retreating"), 1.f, false, false, false, false);

        resetMotion();

    }

    /* ----------------------------------- ACCESSORS ----------------------------------- */

    protected DiscreteCoordinates getScatterPosition() {
        return scatterPosition;
    }

    protected void setScatterPosition(DiscreteCoordinates scatterPosition) {
        this.scatterPosition = scatterPosition;
    }

    protected boolean isPlayerInView() {
        return playerInView;
    }

    protected void setPlayerInView(boolean playerInView) {
        // state check
        stateUpdate = playerInView != this.playerInView;
        this.playerInView = playerInView;
    }

    protected boolean isStateUpdate() {
        return stateUpdate;
    }

    protected DiscreteCoordinates getLastPlayerPosition() {
        return lastPlayerPosition;
    }

    protected boolean isFrightened() {
        return frightened;
    }

    protected void setFrightened(boolean frightened) {
        // state check
        stateUpdate = frightened != this.frightened;
        this.frightened = frightened;
    }

    /**
     * Method to set the ghost as Eaten, sending him back to homePosition
     */
    protected void setEaten() {
//        pause(Ghost.EATEN_TIME);
        isEaten = true;
        path = null;
        movementDuration = BACK_TO_HOME_ANIMATION_DURATION;
        setFrightened(false);
        frightenedTime = FRIGHTENED_TIME;
    }

    @Override
    public void bip(Audio audio) {
        // TODO: find better way
        if (isEaten && !soundHasStarted && maxSounds < 1) {
            ++maxSounds;
            retreatingSound.shouldBeStarted();
            retreatingSound.bip(audio);
            soundHasStarted = true;
        }
    }

    @Override
    public void update(float deltaTime) {
        updateTimer(deltaTime);

        if (reset) {
            if (!hasReset) {
                reset();
            }
            if (timerIsFinished) {
                hasReset = false;
                reset = false;
            }
        }

        if (!paused) {
            updateAnimation(deltaTime);
            if (!isDisplacementOccurs()) {
                currentOrientation = getNextOrientation();
                orientate(currentOrientation);
            }

            if (frightened) {
                if (frightenedTime == FRIGHTENED_TIME) {
                    orientate(currentOrientation.opposite());
                }
                frightenedTime -= deltaTime;
                if (frightenedTime <= 0) {
                    frightenedTime = FRIGHTENED_TIME;
                    setFrightened(false);
                }
            }
            if (isEaten && reachedDestination(homePosition)) {
                currentOrientation = DEFAULT_ORIENTATION;
                movementDuration = ANIMATION_DURATION;
                soundHasStarted = true;
                isEaten = false;
                maxSounds = 0;
            }

            if (!isDisplacementOccurs()) {
                move(movementDuration);
            }
        }

        // Rest
        setPlayerInView(false);
        super.update(deltaTime);
    }

    /**
     * Method to start, stop, and check if timer is finished
     * @param deltaTime elapsed time since last update, in seconds, non-negative
     */
    // TODO: make own class from method...
    private void updateTimer(float deltaTime) {
        if (pauseTime != 0) {
            timer = pauseTime;
            pauseTime = 0;
        }
        if (timer >= 1) {
            timerIsFinished = false;
            timer -= deltaTime;
        } else {
            timerIsFinished = true;
            paused = false;
        }
    }

    /**
     * Reset Ghost, resetting all attributes to initial values
     */
    protected void reset() {
        pause(RESET_TIME);
        resetMotion();
        resetAnimations();
        path = null;
        targetPos = null;
        isEaten = false;
        lastPlayerPosition = null;
        setFrightened(false);
        setPlayerInView(false);
        movementDuration = ANIMATION_DURATION;
        frightenedTime = FRIGHTENED_TIME;
        getOwnerArea().leaveAreaCells(this, getEnteredCells());
        getOwnerArea().leaveAreaCells(this, getLeftCells());
        setCurrentPosition(homePosition.toVector());
        hasReset = true;
    }

    /**
     * Method to update the animations
     * @param deltaTime elapsed time since last update, in seconds, non-negative
     */
    private void updateAnimation(float deltaTime) {
        normalAnimation[currentOrientation.ordinal()].update(deltaTime);
        frightenedAnimation.update(deltaTime);
    }

    /**
     * Method to move Ghost according to current state
     * @return Orientation from path.poll()
     */
    private Orientation getNextOrientation() {
        if (isEaten) {
            return moveToTarget(homePosition);
        }
        if (frightened) {
            return moveToTarget(getTargetWhileFrightened());
        }
        if (playerInView) {
            return moveToTarget(getTargetWhilePlayerInVew());
        }
        if (chase) {
            return moveToTarget(getTargetWhileChaseMode());
        }

        return moveToTarget(getTargetDefault());
    }

    /**
     * Method to know if ghost reached the target position
     * @param targetPos target position of path
     * @return (true) if the current position equals the target position
     */
    private boolean reachedDestination(DiscreteCoordinates targetPos) {
        return targetPos != null && targetPos.equals(getCurrentMainCellCoordinates());
    }

    /**
     * Method to pause update of the ghost
     * @param time the amount of seconds to pause the Ghost
     */
    protected void pause(float time) {
        paused = true;
        pauseTime = time;
    }

    /**
     * Method to rest all animation of Ghost
     */
    private void resetAnimations() {
        for (Animation animation : normalAnimation) {
            animation.reset();
        }
        for (Animation animation : backToHomeAnimation) {
            animation.reset();
        }
        frightenedAnimation.reset();
    }

    /**
     * Method to move Ghost towards the target position
     * @param targetPos target position of path
     * @return the next Orientation from the path or a random orientation if null
     */
    private Orientation moveToTarget(DiscreteCoordinates targetPos) {
        if (!invalidPath(targetPos)) {
            if (path == null || path.isEmpty() || stateUpdate || reachedDestination(this.targetPos)) {
                this.targetPos = targetPos;
                path = SuperPacmanAreaBehavior.areaGraph.shortestPath(getCurrentMainCellCoordinates(), targetPos);
            }

            if (isMoveLegal(path.peek())) {
                return path.poll();
            } else {
                path = null;
            }
        }
        return getRandomOrientation();
    }

    /**
     * Abstract methods to get target while in a specific state
     * @return the target position
     */
    protected abstract DiscreteCoordinates getTargetWhileFrightened();

    protected abstract DiscreteCoordinates getTargetWhilePlayerInVew();

    protected abstract DiscreteCoordinates getTargetWhileChaseMode();

    protected abstract DiscreteCoordinates getTargetDefault();

    /**
     * Method to check if path to targetPos is invalid
     * @param targetPos target position of path
     * @return (true) if the path is invalid
     */
    private boolean invalidPath(DiscreteCoordinates targetPos) {
        return SuperPacmanAreaBehavior.areaGraph.shortestPath(getCurrentMainCellCoordinates(), targetPos) == null ||
                targetPos == null;
    }

    /**
     * Method to check if the desired next Orientation is legal
     * @param orientation the next Orientation
     * @return (true) if the move is legal
     */
    private boolean isMoveLegal(Orientation orientation) {
        return (getPossibleOrientations().contains(orientation));
    }

    /**
     * Method to get a random Orientation from the possible orientations
     * @return a random possible Orientation
     */
    private Orientation getRandomOrientation() {
        List<Orientation> possibleOrientations = getPossibleOrientations();
        if (possibleOrientations.isEmpty()) {
            return currentOrientation.opposite();
        }
        int randomInt = RandomGenerator.getInstance().nextInt(possibleOrientations.size());
        return possibleOrientations.get(randomInt);
    }

    /**
     * Method to get all possible Orientation in the current cell
     * The rules inclue:
     * - no moving backwards, except if dead end
     * - no moving into walls
     * @return a List with all possible orientations
     */
    private List<Orientation> getPossibleOrientations() {
        List<Orientation> possibleOrientations = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            // Check if ghost can move in any directions
            Orientation orientation = Orientation.fromInt(i);
            List<DiscreteCoordinates> jumpedCell =
                    Collections.singletonList(getCurrentMainCellCoordinates().jump(orientation.toVector()));
            if (getOwnerArea().canEnterAreaCells(this, jumpedCell) && orientation != currentOrientation.opposite()) {
                possibleOrientations.add(orientation);
            }
        }
        if (possibleOrientations.isEmpty()) {
            possibleOrientations.add(currentOrientation.opposite());
        }
        return possibleOrientations;
    }

    /**
     * Method to get a random valid position from List of DiscreteCoordinates
     * @param discreteCoordinates the List of coordinates
     * @return a random DiscreteCoordinates from the List
     */
    protected DiscreteCoordinates getRandomValidElement(List<DiscreteCoordinates> discreteCoordinates) {
        int randomInt;
        do {
            randomInt = RandomGenerator.getInstance().nextInt(discreteCoordinates.size());
        } while (invalidPath(discreteCoordinates.get(randomInt)));

        return discreteCoordinates.get(randomInt);
    }

    @Override
    public void draw(Canvas canvas) {
//        if (path != null) {
//            Path graphicPath = new Path(this.getPosition(), new LinkedList<>(path));
//            graphicPath.draw(canvas);
//        }
        if (frightened) {
            frightenedAnimation.draw(canvas);
            glows[FRIGHTENED_GLOW].draw(canvas);
        } else if (isEaten) {
            backToHomeAnimation[currentOrientation.ordinal()].draw(canvas);
        } else {
            normalAnimation[currentOrientation.ordinal()].draw(canvas);
            glows[NORMAL_GLOW].draw(canvas);
        }
    }

    @Override
    public List<DiscreteCoordinates> getCurrentCells() {
        return Collections.singletonList(getCurrentMainCellCoordinates());
    }

    @Override
    public boolean takeCellSpace() {
        return false;
    }

    @Override
    public boolean isCellInteractable() {
        return !isEaten && !paused && !reset;
    }

    @Override
    public boolean isViewInteractable() {
        return false;
    }

    @Override
    public void acceptInteraction(AreaInteractionVisitor v) {
        ((SuperPacmanInteractionVisitor) v).interactWith(this);
    }

    @Override
    public List<DiscreteCoordinates> getFieldOfViewCells() {
        return getCellsFromRange(getCurrentMainCellCoordinates(), 5, false);
    }

    /**
     * Method to get List of cells in a specific range
     * @param initPos  the center position of the range
     * @param range    the range from the initPos
     * @param onlyEdge (true) get only the cells on the edge of the range
     * @return a List of all cells in the specified range
     */
    protected List<DiscreteCoordinates> getCellsFromRange(DiscreteCoordinates initPos, int range, boolean onlyEdge) {
        List<DiscreteCoordinates> cellsInView = new ArrayList<>();
        for (int x = -range; x <= range; x = onlyEdge ? x + (range * 2) : ++x) {
            for (int y = -range; y <= range; ++y) {
                cellsInView.add(initPos.jump(x, y));
            }
        }
        if (onlyEdge) {
            for (int y = -range; y <= range; y += (range * 2)) {
                for (int x = -range; x <= range; ++x) {
                    cellsInView.add(initPos.jump(x, y));
                }
            }
        }

        return cellsInView;
    }

    @Override
    public boolean wantsCellInteraction() {
        return false;
    }

    @Override
    public boolean wantsViewInteraction() {
        return !isEaten && !paused && !reset;
    }

    @Override
    public void interactWith(Interactable other) {
        other.acceptInteraction(ghostHandler);
    }

    /**
     * Interaction handler class for Ghost
     */
    private class GhostInteractionHandler implements SuperPacmanInteractionVisitor {

        @Override
        public void interactWith(SuperPacmanPlayer player) {
            lastPlayerPosition = player.getCurrentCells().get(0);
            setPlayerInView(true);
        }
    }
}