package com.example.strokeapp.rehabilitation;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.strokeapp.R;

public class Rehabilitation extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rehabilitation);
    }

    public void goToEEGTraining(View view) {
        Intent EEGTrainingIntent = new Intent(getApplicationContext(), EEGTraining.class);
        startActivity(EEGTrainingIntent);
    }

    public void goToMotorTraining(View view) {
        Intent motorTrainingIntent = new Intent(getApplicationContext(), MotorTraining.class);
        startActivity(motorTrainingIntent);
    }
}