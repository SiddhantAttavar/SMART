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
import com.example.strokeapp.R;

public class EEGTrainingActivity extends FragmentActivity {

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

