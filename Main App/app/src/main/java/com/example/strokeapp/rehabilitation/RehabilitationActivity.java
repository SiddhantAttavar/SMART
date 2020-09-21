package com.example.strokeapp.rehabilitation;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.MenuItemFragment;
import com.example.strokeapp.R;

@SuppressWarnings("FieldCanBeLocal")
public class RehabilitationActivity extends AppCompatActivity {

    //UI elements
    private MenuItemFragment eegTrainingMenuItemFragment;
    private MenuItemFragment cognitiveNumberTrainingMenuItemFragment;
    private MenuItemFragment cognitiveStroopTrainingMenuItemFragment;
    private ImageView circle;

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rehabilitation);

        //Initialize UI
        circle = findViewById(R.id.circle);
        eegTrainingMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.eeg_training);
        cognitiveNumberTrainingMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.cognitive_number_training);
        cognitiveStroopTrainingMenuItemFragment = (MenuItemFragment) getSupportFragmentManager().findFragmentById(R.id.cognitive_stroop_training);

        //Set up the fragments
        eegTrainingMenuItemFragment.setup(R.drawable.eeg_training, R.string.eeg_training, R.color.rehabilitation_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), EEGTrainingActivity.class)));

        cognitiveNumberTrainingMenuItemFragment.setup(R.drawable.cognitive_number_training, R.string.cognitive_number_training, R.color.rehabilitation_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), CognitiveTrainingNumberActivity.class)));

        cognitiveStroopTrainingMenuItemFragment.setup(R.drawable.cognitive_stroop_training, R.string.cognitive_stroop_training, R.color.rehabilitation_accent_color,
                () -> startActivity(new Intent(getApplicationContext(), CognitiveTrainingStroopActivity.class)));

        circle.getDrawable().setColorFilter(getResources().getColor(R.color.rehabilitation_accent_color), PorterDuff.Mode.SRC_ATOP);
    }
}