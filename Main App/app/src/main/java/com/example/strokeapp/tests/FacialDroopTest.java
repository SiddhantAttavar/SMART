package com.example.strokeapp.tests;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.R;
import com.example.strokeapp.TensorflowModelFragment;

public class FacialDroopTest extends AppCompatActivity {
    //UI elements
    private TextView result;
    private TensorflowModelFragment tfModelFragment;

    private float[][] results = new float[1][2];

    private Runnable postAnalysis = new Runnable() {
        @Override
        public void run() {
            result.setText(String.format(getString(R.string.results_prompt), results[0][0]));
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facial_droop_test);

        //Initialize UI
        result = findViewById(R.id.results);
        result.setText("Results: Stroke probability: ");

        tfModelFragment = (TensorflowModelFragment) getSupportFragmentManager().findFragmentById(R.id.tensorflow_fragment);
        tfModelFragment.setup("facial_droop_model.tflite", false, 440, 440, results, postAnalysis);
    }

    public void captureImage(View view) {
        tfModelFragment.doInference();
    }
}