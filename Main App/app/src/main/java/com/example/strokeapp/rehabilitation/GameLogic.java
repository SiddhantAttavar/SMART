package com.example.strokeapp.rehabilitation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.example.strokeapp.R;

@SuppressWarnings("FieldCanBeLocal")
public class GameLogic extends SurfaceView {

    //We create a new Thread for the game to avoid blocking other processes going on in the main thread
    private Thread thread;

    protected boolean isPlaying = false;

    //We update our game screen at 60Hz
    private final int updateFreq = 1000 / 60;

    //Window size and scaling related windows
    public static float scaleX, scaleY;
    public static int width, height;

    //Background related variables
    public Background bg1, bg2;
    public Paint paint;

    /**
     * This Runnable is called at 60Hz as long as the user is playing the game
     **/
    private Runnable gameRunnable = () -> {
        while (isPlaying) {
            update();
            draw();
            sleep();
        }
    };

    /**
     * Constructor for the Game
     * @param context Context required by SurfaceView
     */
    public GameLogic(Context context) {
        super(context);
    }

    /**
     * Constructor for the game required by SurfaceView
     * @param context Context required by SurfaceView
     * @param attributeSet AttributeSet required by SurfaceView
     */
    public GameLogic(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /**
     * Called when the view is instantiated
     * Here we initialize the properties of the game
     */
    public void onCreate() {
        width = getWidth();
        height = getHeight();
        scaleX = 936f / width;
        scaleY = 757f / height;
        bg1 = new Background(width, height, getResources(), R.drawable.game_background);
        bg2 = new Background(width, height, getResources(), R.drawable.game_background);
        bg2.x = width;
        paint = new Paint();
    }

    /**
     * Called when the user pause/resumes the game by clicking on the pausing/resuming the game or leaving the screen
     *
     * If the game is paused we instantiate the Thread for the game and start it
     * Else it is playing and we need to stop the thread
     * We also update the isPlaying variable appropriately
     */
    public void pause() {
        if (isPlaying) {
            try {
                //We need the pause the game
                isPlaying = false;
                thread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            //We need to resume the game
            isPlaying = true;
            thread = new Thread(gameRunnable);
            thread.start();
        }
    }

    /**
     * Make the thread sleep for updateFreq time to ensure a 60Hz refresh rate
     */
    private void sleep() {
        try {
            Thread.sleep(updateFreq);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the background to create a scrolling effect
     * We use two copies of the same background for this effect and scroll them
     * from one end to the other
     */
    public void update() {
        //Make background move
        bg1.x -= 10 * scaleX;
        bg2.x -= 10 * scaleX;

        //Check if any of the backgrounds has reached the end of the screen
        if (bg1.x + bg1.background.getWidth() < 0) {
            bg1.x = width;
        }
        if (bg2.x + bg2.background.getWidth() < 0) {
            bg2.x = width;
        }
    }

    /**
     * Display the background on the screen using a canvas
     */
    public void draw() {
        //Check if object has been instantiated
        if (getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();
            bg1.displayBackground(canvas, paint);
            bg2.displayBackground(canvas, paint);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    /**
     * GameObject class which can be instantiated as required by the game
     * for each character/game object
     */
    public static class GameObject {

        //Basic properties of the object: position, size, image
        public int x, y;
        public int width, height;
        public Bitmap costume;

        /**
         * Constructor for the GameObject
         * @param resources Get the required resources
         * @param resourceDrawableID Resource Drawable ID for the object's image
         */
        public GameObject(Resources resources, int resourceDrawableID) {
            //Initializing Bitmap
            costume = BitmapFactory.decodeResource(resources, resourceDrawableID);
            width = (int) (costume.getWidth() * GameLogic.scaleX / 10);
            height = (int) (costume.getHeight() * GameLogic.scaleY / 10);
            costume = Bitmap.createScaledBitmap(costume, width, height, false);
            y = height / 2;
            x = (int) (30 * scaleX);
        }

        /**
         * Display the object on the canvas
         * @param canvas Canvas to display the object on
         * @param paint paint to use
         */
        public void displayObject(Canvas canvas, Paint paint) {
            canvas.drawBitmap(costume, x, y, paint);
        }
    }

    /**
     * Background class which can be used by the game to se the background of the game
     */
    public static class Background {

        //Basic properties of the background: position, image
        public int x = 0, y = 0;
        public Bitmap background;

        /**
         * Constructor of the Background to instantiate the background with the width, height and
         * @param width width of the background
         * @param height height of the background
         * @param resources required resources
         * @param resourceDrawableID Resource Drawable ID of the image to be used
         */
        public Background(int width, int height, Resources resources, int resourceDrawableID) {
            background = BitmapFactory.decodeResource(resources, resourceDrawableID);
            background = Bitmap.createScaledBitmap(background, width, height, false);
        }

        /**
         * Displays the background on the canvas
         * @param canvas Canvas to display the background on
         * @param paint paint to use
         */
        public void displayBackground(Canvas canvas, Paint paint) {
            canvas.drawBitmap(background, x, y, paint);
        }
    }
}
