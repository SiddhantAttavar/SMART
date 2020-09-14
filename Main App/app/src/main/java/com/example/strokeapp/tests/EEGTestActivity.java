package com.example.strokeapp.tests;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.R;
import com.example.strokeapp.eeg.EEGFragment;

import static com.example.strokeapp.eeg.EEGFragment.ALPHA_HIGH;
import static com.example.strokeapp.eeg.EEGFragment.ALPHA_LOW;
import static com.example.strokeapp.eeg.EEGFragment.DELTA_HIGH;
import static com.example.strokeapp.eeg.EEGFragment.DELTA_LOW;

@SuppressWarnings({"FieldCanBeLocal", "DefaultLocale"})
public class EEGTestActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView timer, results;
    private EEGFragment eegFragment;

    private EEGFragment.EEGBand[] eegBands = new EEGFragment.EEGBand[] {
        new EEGFragment.EEGBand(DELTA_LOW, DELTA_HIGH, "Delta"),
        new EEGFragment.EEGBand(ALPHA_LOW, ALPHA_HIGH, "Alpha")
    };
    private double DAR;
    private final double STROKE_THRESHOLD = 3.7;

    private CountDownTimer countDownTimer;
    private final long TEST_TIME = (long) (1000 * 60 * 0.25);


    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eeg_test);

        progressBar = findViewById(R.id.progress_bar);
        timer = findViewById(R.id.timer);
        results = findViewById(R.id.result);

        progressBar.setMax((int) (TEST_TIME));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(0, true);
        }
        else {
            progressBar.setProgress(0);
        }

        eegFragment = (EEGFragment) getSupportFragmentManager().findFragmentById(R.id.eeg_fragment);
        assert eegFragment != null;
        eegFragment.setup(eegBands, false, false, () -> {
            DAR = eegBands[0].val / eegBands[1].val;

            EEGTestActivity.this.runOnUiThread(() -> {
                if (DAR > STROKE_THRESHOLD) {
                    results.setText(String.format("%.4f\nStroke: Yes", DAR));
                }
                else {
                    results.setText(String.format("%.4f\nStroke: No", DAR));
                }
            });

            EEGTestActivity.this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Alpha val: " + eegBands[1].val, Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    public void startTest(View view) {
        countDownTimer = new CountDownTimer(TEST_TIME, 1000) {
            @Override
            public void onTick(long timeLeft) {
                long timeDone = TEST_TIME - timeLeft;
                timer.setText(String.valueOf(timeDone / 1000));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress((int) timeDone, true);
                }
                else {
                    progressBar.setProgress((int) timeDone);
                }
            }

            @Override
            public void onFinish() {
                eegFragment.disconnect();

                timer.setText(R.string.test_complete);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress((int) TEST_TIME, true);
                }
                else {
                    progressBar.setProgress((int) TEST_TIME);
                }
            }
        };
        countDownTimer.start();
    }
}