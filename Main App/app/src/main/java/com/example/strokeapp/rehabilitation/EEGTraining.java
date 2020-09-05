package com.example.strokeapp.rehabilitation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.fragment.app.FragmentActivity;

import com.example.strokeapp.EEGProcessor;
import com.example.strokeapp.GameLogic;
import com.example.strokeapp.MuseFragment;
import com.example.strokeapp.R;

public class EEGTraining extends FragmentActivity {

    private EEGProcessor eegProcessor;
    private EEGGame eegGame;
    private ImageButton playButton;
    private Button connectButton;

    private boolean isPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eeg_training);
        eegGame = findViewById(R.id.game);
        playButton = findViewById(R.id.pause_button);
        connectButton = findViewById(R.id.connect_button);
        //eegProcessor = new EEGProcessor(this, connectButton);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        eegGame.onCreate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPlaying = false;
        pause();
    }

    public void pauseClicked(View view) {
        pause();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void pause() {
        if (isPlaying) {
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        }
        else {
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
        }
        eegGame.pause();
        isPlaying = !isPlaying;
    }
}

class EEGGame extends GameLogic {
    private Plane plane;

    public EEGGame(Context context) {
        super(context);
    }

    public EEGGame(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        plane = new Plane(getResources(), R.drawable.plane1, R.drawable.plane2);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        plane.goingUp = motionEvent.getAction() == MotionEvent.ACTION_DOWN;
        return true;
    }

    @Override
    public void update() {
        super.update();
        int y = plane.getObject().y;
        if (plane.goingUp) {
            y = (int) Math.max(0, y - (10 * scaleY));
        }
        else {
            y = (int) Math.min(height - plane.height, y + (10 * scaleY));
        }
        plane.getObject().y = y;
    }

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

    private class Plane {
        GameObject state1, state2;
        private int state = 1;
        public int height;
        public boolean goingUp = false;

        Plane(Resources resources, int resourceDrawable1, int resourceDrawable2) {
            state1 = new GameObject(resources, resourceDrawable1);
            state2 = new GameObject(resources, resourceDrawable2);
            height = state1.height;
        }

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