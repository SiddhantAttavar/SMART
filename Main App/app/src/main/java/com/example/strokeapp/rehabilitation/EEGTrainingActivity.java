package com.example.strokeapp.rehabilitation;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.fragment.app.FragmentActivity;

import com.example.strokeapp.eeg.EEGFragment;
import com.example.strokeapp.eeg.EEGProcessor;
import com.example.strokeapp.R;

@SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression"})
public class EEGTrainingActivity extends FragmentActivity {

    private EEGGame eegGame;
    private ImageButton playButton;
    private Button connectButton;
    private View ssvep;

    private EEGFragment eegFragment;
    double ABR = 0;
    private EEGFragment.EEGBand[] eegBands = new EEGFragment.EEGBand[] {
            new EEGFragment.EEGBand(EEGProcessor.ALPHA_LOW, EEGProcessor.ALPHA_HIGH, "Alpha"),
            new EEGFragment.EEGBand(EEGProcessor.BETA_LOW, EEGProcessor.BETA_HIGH, "Beta")
    };

    public boolean isPlaying = true;

    private Handler handler;
    private final int SSVEP_FREQ = 10;
    private final long SSVEP_TIME = 1000 / 2 / SSVEP_FREQ;
    private boolean ssvepOn = false;
    private int green, white;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (!isPlaying) {
                if (ssvepOn) {
                    ssvep.setBackgroundColor(white);
                }
                else {
                    ssvep.setBackgroundColor(green);
                }
                ssvepOn = !ssvepOn;
                handler.postDelayed(this, SSVEP_TIME);
            }
            else {
                ssvep.setBackgroundColor(green);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eeg_training);
        eegGame = findViewById(R.id.game);
        playButton = findViewById(R.id.pause_button);
        ssvep = findViewById(R.id.ssvep_view);

        green = getResources().getColor(R.color.green);
        white = getResources().getColor(R.color.white);

        handler = new Handler(getMainLooper());

        eegFragment = (EEGFragment) getSupportFragmentManager().findFragmentById(R.id.eeg_fragment);
        assert eegFragment != null;
        eegFragment.setup(eegBands, true, true, this::analyze);
    }

    private void analyze() {
        //We calculate the ABR and move the plane up only if the ABR has increased
        double currentABR = eegBands[0].val / eegBands[1].val;
        eegGame.plane.goingUp = (currentABR > ABR);
        ABR = currentABR;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        eegGame.onCreate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        eegFragment.onPause();
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
            handler.postDelayed(runnable, SSVEP_TIME);
        }
        else {
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));
        }
        eegGame.pause();
        isPlaying = !isPlaying;
    }
}

