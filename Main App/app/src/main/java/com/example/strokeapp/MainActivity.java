package com.example.strokeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.rehabilitation.RehabilitationActivity;
import com.example.strokeapp.results.ResultsActivity;
import com.example.strokeapp.tests.TestsActivity;

import ai.fritz.core.Fritz;

public class MainActivity extends AppCompatActivity {

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Configure the Fritz AI API for Pose estimation
        Fritz.configure(this, "486c39c85612487d8f7eaac8d40c96a3");
    }

    /**
     * Go to Tests activity
     */
    public void goToTests(View view) {
        startActivity(new Intent(getApplicationContext(), TestsActivity.class));
    }

    /**
     * Go to Rehabilitation activity
     */
    public void goToRehabilitation(View view) {
        startActivity(new Intent(getApplicationContext(), RehabilitationActivity.class));
    }

    /**
     * Go to Results activity
     */
    public void goToResults(View view) {
        startActivity(new Intent(getApplicationContext(), ResultsActivity.class));
    }
}