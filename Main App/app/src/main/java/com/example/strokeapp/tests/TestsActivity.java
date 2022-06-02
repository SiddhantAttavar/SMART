package com.example.strokeapp.tests;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.MenuItemFragment;
import com.example.strokeapp.R;

@SuppressWarnings("FieldCanBeLocal")
public class TestsActivity extends AppCompatActivity {

    //UI elements
    private MenuItemFragment facialDroopMenuItemFragment;
    private MenuItemFragment armWeaknessMenuItemFragment;
    private MenuItemFragment eegTestMenuItemFragment;
    private ImageView circle;

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tests);

        //Initialize UI
        circle = findViewById(R.id.circle);
        facialDroopMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.facial_droop);
        armWeaknessMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.arm_weakness);
        eegTestMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.eeg_test);

        //Set up the fragments
        facialDroopMenuItemFragment.setup(R.drawable.facial_droop, R.string.facial_droop, R.color.tests_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), FacialDroopTestActivity.class)));

        armWeaknessMenuItemFragment.setup(R.drawable.arm_weakness, R.string.arm_weakness, R.color.tests_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), ArmWeaknessTestActivity.class)));

        eegTestMenuItemFragment.setup(R.drawable.eeg_test, R.string.eeg_test, R.color.tests_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), EEGTestActivity.class)));

        circle.getDrawable().setColorFilter(getResources().getColor(R.color.tests_accent_color), PorterDuff.Mode.SRC_ATOP);
    }
}