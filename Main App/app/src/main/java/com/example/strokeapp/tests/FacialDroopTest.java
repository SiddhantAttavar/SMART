package com.example.strokeapp.tests;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.R;

public class FacialDroopTest extends AppCompatActivity {
    //UI elements
    private TextView results;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facial_droop_test);

        //Initialize UI
        results = findViewById(R.id.results);
        imageView = findViewById(R.id.display_image_view);
    }
}