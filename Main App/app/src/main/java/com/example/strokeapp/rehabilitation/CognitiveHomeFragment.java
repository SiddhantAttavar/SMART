package com.example.strokeapp.rehabilitation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.strokeapp.R;

@SuppressWarnings({"unused", "RedundantSuppression", "FieldCanBeLocal"})
public class CognitiveHomeFragment extends Fragment {

    private FragmentManager fragmentManager;
    private View root;
    private Button memory, languageVision, mentalAptitude;

    public CognitiveHomeFragment() {
        // Required empty public constructor
    }

    public CognitiveHomeFragment(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_cognitive_home, container, false);
        memory = root.findViewById(R.id.memory);
        languageVision = root.findViewById(R.id.language);
        mentalAptitude = root.findViewById(R.id.mental_aptitude);

        memory.setOnClickListener((View view) -> goToFragment(new CognitiveMemoryFragment(true)));
        languageVision.setOnClickListener((View view) -> goToFragment(new CognitiveLanguageVisionFragment()));
        mentalAptitude.setOnClickListener((View view) -> goToFragment(new CognitiveMentalAptitudeFragment()));

        return root;
    }
    private void goToFragment(Fragment fragment) {
        fragmentManager.beginTransaction().replace(R.id.container, fragment).addToBackStack(null).commit();
    }
}