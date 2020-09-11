package com.example.strokeapp.rehabilitation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.strokeapp.R;

public class RehabilitationActivity extends AppCompatActivity {

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rehabilitation);
    }

    /**
     * Go to EEG training activity
     */
    public void goToEEGTraining(View view) {
        startActivity(new Intent(getApplicationContext(), EEGTrainingActivity.class));
    }

    /**
     * Go to Motor training activity
     */
    public void goToMotorTraining(View view) {
        startActivity(new Intent(getApplicationContext(), MotorTrainingActivity.class));
    }

    /**
     * Go to Congitive training activity
     */
    public void goToCogntiveTraining(View view) {
        startActivity(new Intent(getApplicationContext(), CognitiveTrainingActivity.class));
    }
}