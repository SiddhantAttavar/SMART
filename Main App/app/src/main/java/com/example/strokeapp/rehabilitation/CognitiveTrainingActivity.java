package com.example.strokeapp.rehabilitation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.strokeapp.R;

import java.util.Random;

@SuppressWarnings({"InnerClassMayBeStatic", "FieldCanBeLocal"})
public class CognitiveTrainingActivity extends AppCompatActivity {

    private FragmentManager fragmentManager = getSupportFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cognitive_training);

        fragmentManager.beginTransaction().replace(R.id.container, new CognitiveHomeFragment(fragmentManager)).commit();
    }
}