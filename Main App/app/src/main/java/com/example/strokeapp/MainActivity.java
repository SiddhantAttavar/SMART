package com.example.strokeapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.rehabilitation.Rehabilitation;
import com.example.strokeapp.results.Results;
import com.example.strokeapp.tests.Tests;

import ai.fritz.core.Fritz;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fritz.configure(this, "486c39c85612487d8f7eaac8d40c96a3");
    }

    /**
     * Go to Tests activity
     */
    public void goToTests(View view) {
        Intent testsIntent = new Intent(getApplicationContext(), Tests.class);
        startActivity(testsIntent);
    }

    /**
     * Go to Rehabilitation activity
     */
    public void goToRehabilitation(View view) {
        Intent rehabilitationIntent = new Intent(getApplicationContext(), Rehabilitation.class);
        startActivity(rehabilitationIntent);
    }

    /**
     * Go to Results activity
     */
    public void goToResults(View view) {
        Intent resultsIntent = new Intent(getApplicationContext(), Results.class);
        startActivity(resultsIntent);
    }
}