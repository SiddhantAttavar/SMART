package com.example.strokeapp.results;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.strokeapp.R;

@SuppressWarnings({"FieldCanBeLocal", "ConstantConditions"})
public class ResultsFragment extends Fragment {

    //UI elements
    private View root;
    private CardView cardView;
    private TextView name, id, results;

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

        if (type == ResultsActivity.TESTS) {
            cardView.setBackgroundColor(getContext().getResources().getColor(R.color.dark_green));
        }
        else {
            cardView.setBackgroundColor(getContext().getResources().getColor(R.color.dark_blue));
        }

        cardView.setCardElevation(15);
        cardView.setRadius(10);
        this.name.setText(nameVal);
        this.id.setText(getString(R.string.id, idVal));
        results.setText(getString(R.string.result, result));

        return root;
    }

    /**
     * Sets up the frgament
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

        try {
            if (type == ResultsActivity.TESTS) {
                cardView.setBackgroundColor(getContext().getResources().getColor(R.color.dark_green));
            }
            else {
                cardView.setBackgroundColor(getContext().getResources().getColor(R.color.light_blue));
            }

            cardView.setCardElevation(15);
            cardView.setRadius(10);
            this.name.setText(nameVal);
            this.id.setText(getString(R.string.id, idVal));
            results.setText(getString(R.string.result, result));
        }
        catch (NullPointerException ignored) {
            //The UI has not been initialized and we need to wait for the on create view function to be called
        }
    }
}