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
import com.example.strokeapp.results.ResultsActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import static com.example.strokeapp.eeg.EEGFragment.ALPHA_HIGH;
import static com.example.strokeapp.eeg.EEGFragment.ALPHA_LOW;
import static com.example.strokeapp.eeg.EEGFragment.DELTA_HIGH;
import static com.example.strokeapp.eeg.EEGFragment.DELTA_LOW;
import static com.example.strokeapp.eeg.EEGFragment.HIGH_PASS;
import static com.example.strokeapp.eeg.EEGFragment.LOW_PASS;

@SuppressWarnings({"FieldCanBeLocal", "DefaultLocale"})
public class EEGTestActivity extends AppCompatActivity {

    //UI elements
    private ProgressBar progressBar;
    private TextView timer, results;
    private EEGFragment eegFragment;

    //In order to calculate the DAR we have to monitor the delta and alpha bands
    private EEGFragment.EEGBand[] eegBands = new EEGFragment.EEGBand[] {
        new EEGFragment.EEGBand(DELTA_LOW, DELTA_HIGH, "Delta"),
        new EEGFragment.EEGBand(ALPHA_LOW, ALPHA_HIGH, "Alpha")
    };
    private double delta, alpha;

    //We define the STROKE_THRESHOLD to be 3.7 as mentioned in studies
    private final double STROKE_THRESHOLD = 3.7;

    //Time for the test is 1 min
    private final long TEST_TIME = (long) (1000 * 60 * 0.25);

    /**
     * Countdown timer
     */
    private CountDownTimer countDownTimer = new CountDownTimer(TEST_TIME, 1000) {

        /**
         * Called every second and updates the UI with the new time
         * @param timeLeft Time left till end
         */
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

        /**
         * Called when the timer finishes
         * Disconnects the device and perform analysis
         */
        @Override
        public void onFinish() {
            eegFragment.disconnect();
            endTest();

            timer.setText(R.string.test_complete);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress((int) TEST_TIME, true);
            }
            else {
                progressBar.setProgress((int) TEST_TIME);
            }
        }
    };

    /**
     * Called when the data is received
     */
    private Runnable dataAnalyzer = () -> {
        delta += eegBands[0].val;
        alpha += eegBands[1].val;

        double[] amplitudes = eegFragment.getFFTResults();
        double[] frequencies = eegFragment.getFrequencies();
    };

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eeg_test);

        //Initialize the UI
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

        //Setup the EEG Fragment
        eegFragment.setup(eegBands, true, true, dataAnalyzer);
    }

    /**
     * Called when the user exits the screen
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    /**
     * Called when the user clicks the start test button
     * Starts the test
     */
    public void startTest(View view) {
        if (!eegFragment.isConnected()) {
            Toast.makeText(this, "Please connect the EEG headset", Toast.LENGTH_SHORT).show();
            return;
        }
        countDownTimer.start();
    }

    private void endTest() {
        double DAR = delta / alpha;

        String result;
        if (DAR > STROKE_THRESHOLD) {
            result = getString(R.string.stroke_test_result, "Yes");
        }
        else {
            result = getString(R.string.stroke_test_result, "No");
        }
        result = getString(R.string.stroke_test_result, "No");

        String finalResult = result;
        EEGTestActivity.this.runOnUiThread(() -> results.setText(   finalResult));
        ResultsActivity.log(EEGTestActivity.this, ResultsActivity.TESTS, getString(R.string.eeg_test), result);
    }
}