package com.example.strokeapp.tests;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.strokeapp.R;

public class Tests extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tests);
    }

    /**
     * Go to Facial Droop Test activity
     */
    public void goToFacialDroopTest(View view) {
        Intent facialDroopTestIntent = new Intent(getApplicationContext(), FacialDroopTest.class);
        startActivity(facialDroopTestIntent);
    }

    /**
     * Go to Arm Weakness Test activity
     */
    public void goToArmWeaknessTest(View view) {
        Intent armWeaknessTestIntent = new Intent(getApplicationContext(), ArmWeaknessTest.class);
        startActivity(armWeaknessTestIntent);
    }

    /**
     * Go to EEG Test activity
     */
    public void goToEEGTest(View view) {
        Intent EEGTestIntent = new Intent(getApplicationContext(), EEGTest.class);
        startActivity(EEGTestIntent);
    }
}