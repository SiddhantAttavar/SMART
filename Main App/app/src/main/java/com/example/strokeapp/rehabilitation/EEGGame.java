package com.example.strokeapp.rehabilitation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import com.example.strokeapp.R;
import com.example.strokeapp.results.ResultsActivity;

@SuppressLint({"ClickableViewAccessibility", "DefaultLocale"})
public class EEGGame extends GameLogic {

    //The plane object
    public Plane plane;

    //Records the time when the game started
    private long startTime;

    /**
     * Constructor for the game
     * @param context Context of the activity using the game
     * @param attributeSet Attribute set
     */
    public EEGGame(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /**
     * Called when the game is created
     */
    @Override
    public void onCreate() {
        super.onCreate();
        //Instantiates the plane object
        plane = new Plane(getResources(), R.drawable.plane1, R.drawable.plane2);
    }

    /**
     * Called when the user touches the game screen
     * @param motionEvent Motion event describing the touch event
     */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        plane.goingUp = motionEvent.getAction() == MotionEvent.ACTION_DOWN;
        return true;
    }

    /**
     * Updates the position of the objects in the screen
     */
    @Override
    public void update() {
        super.update();
        int y = plane.getObject().y;
        if (plane.goingUp) {
            y = (int) Math.max(0, y - (10 * scaleY));
        }
        else {
            if (y + (10 * scaleY) < height - plane.height) {
                //The plane has hit the ground and we need to finish the game
                pause();
            }
            else {
                y = (int) (y + (10 * scaleY));
            }
        }
        plane.getObject().y = y;
    }

    /**
     * Draws the game objects on the screen
     */
    @Override
    public void draw() {
        //Check if object has been instantiated
        if (getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();
            bg1.displayBackground(canvas, paint);
            bg2.displayBackground(canvas, paint);
            plane.getObject().displayObject(canvas, paint);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    /**
     * Called when the user pause/resumes the game by clicking on the pausing/resuming the game or leaving the screen
     * We log the total time that the user managed to play the game for
     */
    @Override
    public void pause() {
        super.pause();
        if (isPlaying) {
            long currentTime = System.currentTimeMillis();
            long totalTime = (currentTime - startTime) / 1000;

            String time = String.format("%d min %d  sec", totalTime / 60, totalTime % 60);
            ResultsActivity.log(getContext(), ResultsActivity.REHABILITATION, "EEG Training", time);
            Toast.makeText(getContext(), String.format("Game Over. Time: %s", time), Toast.LENGTH_LONG).show();
        }
        else {
            startTime = System.currentTimeMillis();
        }
    }

    /**
     * Plane class which contains two game objects for the 2 states to create an illusion of a moving plane
     */
    public static class Plane {

        //Variables defining the class including the two states the
        //height of the plane and whether the plane is going up
        private GameObject state1, state2;
        private int state = 1;
        public int height;
        public boolean goingUp = false;

        /**
         * Constructor for the plane class
         * @param resources Resources to find the drawable resources for the plane image
         * @param resourceDrawable1 Resource drawable id of the first state
         * @param resourceDrawable2 Resource drawable is of the second state
         */
        Plane(Resources resources, int resourceDrawable1, int resourceDrawable2) {
            state1 = new GameObject(resources, resourceDrawable1);
            state2 = new GameObject(resources, resourceDrawable2);
            height = state1.height;
        }

        /**
         * Returns the current state of the plane
         * @return Game object of the state that the plane is in
         */
        public GameObject getObject() {
            if (state == 1) {
                state = 2;
                return state1;
            }
            else {
                state = 1;
                return state2;
            }
        }
    }
}
