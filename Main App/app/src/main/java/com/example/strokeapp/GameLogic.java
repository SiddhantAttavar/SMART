package com.example.strokeapp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class GameLogic extends SurfaceView {
    private Thread thread;

    private boolean isPlaying = false;

    private final int updateFreq = 1000 / 60;

    public static float scaleX, scaleY;
    public static int width, height;

    public Background bg1, bg2;
    public Paint paint;

    private Runnable gameRunnable = new Runnable() {
        @Override
        public void run() {
            while (isPlaying) {
                update();
                draw();
                sleep();
            }
        }
    };

    public GameLogic(Context context) {
        super(context);
    }

    public GameLogic(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void onCreate() {
        width = getWidth();
        height = getHeight();
        scaleX = 936f / width;
        scaleY = 757f / height;
        bg1 = new Background(width, height, getResources());
        bg2 = new Background(width, height, getResources());
        bg2.x = width;
        paint = new Paint();
    }

    public void pause() {
        if (isPlaying) {
            try {
                isPlaying = false;
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            isPlaying = true;
            thread = new Thread(gameRunnable);
            thread.start();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(updateFreq);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

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

    public void draw() {
        //Check if object has been instantiated
        if (getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();
            bg1.displayBackground(canvas, paint);
            bg2.displayBackground(canvas, paint);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    public class GameObject {
        public int x, y;
        public int width, height;
        public Bitmap costume;

        public GameObject(Resources resources, int resourceDrawable) {
            //Initializing Bitmap
            costume = BitmapFactory.decodeResource(resources, resourceDrawable);
            width = (int) (costume.getWidth() * GameLogic.scaleX / 10);
            height = (int) (costume.getHeight() * GameLogic.scaleY / 10);
            costume = Bitmap.createScaledBitmap(costume, width, height, false);
            y = height / 2;
            x = (int) (30 * scaleX);
        }

        public void displayObject(Canvas canvas, Paint paint) {
            canvas.drawBitmap(costume, x, y, paint);
        }
    }

    public class Background {
        public int x = 0, y = 0;
        public Bitmap background;

        public Background(int width, int height, Resources resources) {
            background = BitmapFactory.decodeResource(resources, R.drawable.game_background);
            background = Bitmap.createScaledBitmap(background, width, height, false);
        }

        public void displayBackground(Canvas canvas, Paint paint) {
            canvas.drawBitmap(background, x, y, paint);
        }
    }
}
