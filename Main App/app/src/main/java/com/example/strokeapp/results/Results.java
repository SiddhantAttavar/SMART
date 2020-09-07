package com.example.strokeapp.results;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.EEGProcessor;
import com.example.strokeapp.R;

@SuppressLint("DefaultLocale")
public class Results extends AppCompatActivity {

    TextView results;
    Button connectButton;
    EEGProcessor eegProcessor = null;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        results = findViewById(R.id.results_view);
        connectButton = findViewById(R.id.connect_button);
    }

    public void completeTest(View view) {
        EEGProcessor.EEGBand[] eegBands = new EEGProcessor.EEGBand[] {
                new EEGProcessor.EEGBand(EEGProcessor.DELTA_LOW, EEGProcessor.DELTA_HIGH, "Delta"),
                new EEGProcessor.EEGBand(EEGProcessor.THETA_LOW, EEGProcessor.THETA_HIGH, "Theta"),
                new EEGProcessor.EEGBand(EEGProcessor.ALPHA_LOW, EEGProcessor.ALPHA_HIGH, "Alpha"),
                new EEGProcessor.EEGBand(EEGProcessor.BETA_LOW, EEGProcessor.BETA_HIGH, "Beta")
        };

        results.setText("Complete Test: \n");
        eegProcessor = new EEGProcessor(this, connectButton, true, () -> {
            for (EEGProcessor.EEGBand eegBand: eegBands) {
                Results.this.runOnUiThread(() -> results.append(String.format("%s Relative: %.2f\n", eegBand.bandName, eegProcessor.getRelativeBandpower(eegBand))));
            }
        }, eegBands);
        eegProcessor.test = true;
    }

    public void realTimeTest(View view) {
        EEGProcessor.EEGBand[] eegBands = new EEGProcessor.EEGBand[] {
                new EEGProcessor.EEGBand(EEGProcessor.DELTA_LOW, EEGProcessor.DELTA_HIGH, "Delta"),
                new EEGProcessor.EEGBand(EEGProcessor.THETA_LOW, EEGProcessor.THETA_HIGH, "Theta"),
                new EEGProcessor.EEGBand(EEGProcessor.ALPHA_LOW, EEGProcessor.ALPHA_HIGH, "Alpha"),
                new EEGProcessor.EEGBand(EEGProcessor.BETA_LOW, EEGProcessor.BETA_HIGH, "Beta")
        };

        results.setText("Real Time Test: \n");
        eegProcessor = new EEGProcessor(this, connectButton, true, () -> {
            count++;
            if (count == 1024) {
                for (EEGProcessor.EEGBand eegBand: eegBands) {
                    Results.this.runOnUiThread(() -> results.append(String.format("%s Relative: %.2f\n", eegBand.bandName, eegProcessor.getRelativeBandpower(eegBand))));
                }
            }
        }, eegBands);

        eegProcessor.test = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (eegProcessor != null) {
            eegProcessor.onPause();
            eegProcessor = null;
        }
    }
}