package com.example.strokeapp.tests;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.EEGProcessor;
import com.example.strokeapp.R;

@SuppressWarnings({"FieldCanBeLocal", "DefaultLocale"})
public class EEGTestActivity extends AppCompatActivity {

    private Button connect;
    private ProgressBar progressBar;
    private TextView timer, results;

    private EEGProcessor eegProcessor;
    private EEGProcessor.EEGBand[] eegBands = new EEGProcessor.EEGBand[] {
        new EEGProcessor.EEGBand(EEGProcessor.DELTA_LOW, EEGProcessor.DELTA_HIGH, "Delta"),
        new EEGProcessor.EEGBand(EEGProcessor.ALPHA_LOW, EEGProcessor.ALPHA_HIGH, "Alpha")
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

        connect = findViewById(R.id.connect);
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

        eegProcessor = new EEGProcessor(this, eegBands, connect, false, false, () -> {
            DAR = eegBands[0].val / eegBands[1].val;

            EEGTestActivity.this.runOnUiThread(() -> {
                if (DAR <= STROKE_THRESHOLD) {
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
        eegProcessor.onPause();
        countDownTimer.cancel();
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
                eegProcessor.setRunRealTime(false);

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