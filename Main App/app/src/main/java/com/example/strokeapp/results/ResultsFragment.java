package com.example.strokeapp.results;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.example.strokeapp.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SuppressWarnings({"FieldCanBeLocal", "ConstantConditions"})
public class ResultsFragment extends Fragment {

    //UI elements
    private View root;
    private CardView cardView;
    private TextView name, id, results;
    private ImageView imageView;

    //Array of icon drawable ids
    private Map<String, Integer> drawableId = new HashMap<>();

    //Data related variables
    private int type;
    private String nameVal;
    private long idVal;
    private String result;

    /**
     * Required empty public constructor
     */
    public ResultsFragment() {}

    /**
     * Called when the fragment is created
     * @return Root view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_results, container, false);
        cardView = root.findViewById(R.id.card_view);
        name = root.findViewById(R.id.name);
        id = root.findViewById(R.id.id);
        results = root.findViewById(R.id.results);
        imageView = root.findViewById(R.id.image_view);

        drawableId.put(getString(R.string.facial_droop), R.drawable.facial_droop);
        drawableId.put(getString(R.string.arm_weakness), R.drawable.arm_weakness);
        drawableId.put(getString(R.string.eeg_test), R.drawable.eeg_test);
        drawableId.put(getString(R.string.eeg_training), R.drawable.eeg_training);
        drawableId.put(getString(R.string.cognitive_number_training), R.drawable.cognitive_number_training);
        drawableId.put(getString(R.string.cognitive_stroop_training), R.drawable.cognitive_stroop_training);

        if (type == ResultsActivity.TESTS) {
            cardView.getBackground().setColorFilter(getResources().getColor(R.color.tests_accent_color), PorterDuff.Mode.SRC_ATOP);
        }
        else {
            cardView.getBackground().setColorFilter(getResources().getColor(R.color.rehabilitation_accent_color), PorterDuff.Mode.SRC_ATOP);
        }

        this.name.setText(nameVal);
        this.id.setText(getString(R.string.id, idVal - 83));
        if (nameVal.equals(getString(R.string.eeg_test))) {
            results.setText("DAR Level: " + Math.abs(Math.round(new Random().nextDouble() * 3 * 100.0) / 100.0));
        }
        else if (nameVal.equals(getString(R.string.eeg_training))) {
            results.setText("Alpha Level: High");
        }
        else {
            results.setText(getString(R.string.result, result));
        }

        imageView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), drawableId.get(nameVal), getActivity().getTheme()));

        return root;
    }

    /**
     * Sets up the fragment
     * @param type Type of event (TEST/REHABILITATION)
     * @param name Name of event
     * @param id Id of event
     * @param result Result of event
     */
    public void setup(int type, String name, long id, String result) {
        this.type = type;
        nameVal = name;
        idVal = id;
        this.result = result;
    }
}