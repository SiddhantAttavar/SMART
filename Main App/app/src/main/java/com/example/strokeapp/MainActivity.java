package com.example.strokeapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.rehabilitation.EEGTrainingActivity;
import com.example.strokeapp.rehabilitation.RehabilitationActivity;
import com.example.strokeapp.results.ResultsActivity;
import com.example.strokeapp.tests.EEGTestActivity;
import com.example.strokeapp.tests.TestsActivity;

public class MainActivity extends AppCompatActivity {

    //UI elements
    MenuItemFragment testsMenuItemFragment;
    MenuItemFragment rehabilitationMenuItemFragment;
    MenuItemFragment resultsMenuItemFragment;

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize UI
        testsMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.tests);
        rehabilitationMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.rehabilitation);
        resultsMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.results);

        //Set up the fragments
        testsMenuItemFragment.setup(R.drawable.tests, R.string.test, R.color.tests_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), EEGTestActivity.class)));

        rehabilitationMenuItemFragment.setup(R.drawable.rehabilitation, R.string.rehabilitation, R.color.rehabilitation_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), EEGTrainingActivity.class)));

        resultsMenuItemFragment.setup(R.drawable.results, R.string.results, R.color.results_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), ResultsActivity.class)));
    }
}