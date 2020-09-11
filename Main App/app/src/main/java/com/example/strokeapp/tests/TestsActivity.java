package com.example.strokeapp.tests;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.strokeapp.R;

public class TestsActivity extends AppCompatActivity {

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tests);
    }

    /**
     * Go to Facial Droop Test activity
     */
    public void goToFacialDroopTest(View view) {
        startActivity(new Intent(getApplicationContext(), FacialDroopTestActivity.class));
    }

    /**
     * Go to Arm Weakness Test activity
     */
    public void goToArmWeaknessTest(View view) {
        startActivity(new Intent(getApplicationContext(), ArmWeaknessTestActivity.class));
    }

    /**
     * Go to EEG Test activity
     */
    public void goToEEGTest(View view) {
        startActivity(new Intent(getApplicationContext(), EEGTestActivity.class));
    }
}