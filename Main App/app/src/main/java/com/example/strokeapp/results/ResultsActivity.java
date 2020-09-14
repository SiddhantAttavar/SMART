package com.example.strokeapp.results;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.R;
import com.example.strokeapp.eeg.EEGFragment;
import com.example.strokeapp.eeg.EEGProcessor;

@SuppressLint("DefaultLocale")
public class ResultsActivity extends AppCompatActivity {

    TextView results;
    EEGFragment eegFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        results = findViewById(R.id.results_view);

        eegFragment = (EEGFragment) getSupportFragmentManager().findFragmentById(R.id.eeg_fragment);
    }

    public void completeTest(View view) {
        EEGFragment.EEGBand[] eegBands = new EEGFragment.EEGBand[] {
                new EEGFragment.EEGBand(EEGProcessor.DELTA_LOW, EEGProcessor.DELTA_HIGH, "Delta"),
                new EEGFragment.EEGBand(EEGProcessor.THETA_LOW, EEGProcessor.THETA_HIGH, "Theta"),
                new EEGFragment.EEGBand(EEGProcessor.ALPHA_LOW, EEGProcessor.ALPHA_HIGH, "Alpha"),
                new EEGFragment.EEGBand(EEGProcessor.BETA_LOW, EEGProcessor.BETA_HIGH, "Beta")
        };

        results.setText("Complete Test: \n");
        assert eegFragment != null;
        eegFragment.setup(eegBands, false, false, () -> {
            for (EEGFragment.EEGBand eegBand: eegBands) {
                ResultsActivity.this.runOnUiThread(() -> results.append(String.format("%s Relative: %.2f\n", eegBand.bandName, eegFragment.getRelativeBandpower(eegBand))));
            }
        });
    }

    public void realTimeTest(View view) {
        EEGFragment.EEGBand[] eegBands = new EEGFragment.EEGBand[] {
                new EEGFragment.EEGBand(EEGProcessor.DELTA_LOW, EEGProcessor.DELTA_HIGH, "Delta"),
                new EEGFragment.EEGBand(EEGProcessor.THETA_LOW, EEGProcessor.THETA_HIGH, "Theta"),
                new EEGFragment.EEGBand(EEGProcessor.ALPHA_LOW, EEGProcessor.ALPHA_HIGH, "Alpha"),
                new EEGFragment.EEGBand(EEGProcessor.BETA_LOW, EEGProcessor.BETA_HIGH, "Beta")
        };

        results.setText("Real Time Test: \n");
        assert eegFragment != null;
        eegFragment.setup(eegBands, true, false, () -> {
            StringBuilder msg = new StringBuilder("EEG Bandpowers (Relative): \n");
            for (EEGFragment.EEGBand eegBand: eegBands) {
                msg.append(String.format("%s Relative: %.4f\n", eegBand.bandName, eegFragment.getRelativeBandpower(eegBand)));
            }
            ResultsActivity.this.runOnUiThread(() -> results.setText(msg.toString()));
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (eegFragment != null) {
            eegFragment.onPause();
            eegFragment = null;
        }
    }
}