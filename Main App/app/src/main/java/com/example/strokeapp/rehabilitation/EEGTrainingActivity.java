package com.example.strokeapp.rehabilitation;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.example.strokeapp.R;
import com.example.strokeapp.eeg.EEGFragment;

@SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression", "UseCompatLoadingForDrawables", "DefaultLocale", "SpellCheckingInspection"})
public class EEGTrainingActivity extends FragmentActivity {

    private EEGGame eegGame;
    private ImageButton playButton;
    private Button connectButton;
    private View ssvep;

    private EEGFragment eegFragment;
    private double ABR = 0;
    private EEGFragment.EEGBand[] eegBands = new EEGFragment.EEGBand[] {
            new EEGFragment.EEGBand(EEGFragment.ALPHA_LOW, EEGFragment.ALPHA_HIGH, "Alpha"),
            new EEGFragment.EEGBand(EEGFragment.BETA_LOW, EEGFragment.BETA_HIGH, "Beta")
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

        eegGame.activity = EEGTrainingActivity.this;

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

    private void pause() {
        if (isPlaying) {
            if (eegFragment.isConnected()) {
                playButton.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                handler.postDelayed(runnable, SSVEP_TIME);

                eegGame.pause();
                isPlaying = false;
            }
            else {
                Toast.makeText(this, "Please connect the EEG headset", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            playButton.setImageDrawable(getResources().getDrawable(R.drawable.play));

            eegGame.pause();
            isPlaying = true;
        }
    }
}

