package com.example.strokeapp;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

@SuppressWarnings({"UseCompatLoadingForDrawables", "FieldCanBeLocal", "ConstantConditions"})
public class MenuItemFragment extends Fragment {

    //UI elements
    private View root;
    private ImageView circle;
    private TextView textView;
    private ImageView imageView;

    //Ids for the drawable picture, string and color
    private int drawableId;
    private int stringId;
    private int colorId;

    //On click listener
    private View.OnClickListener onClickListener;

    /**
     * Required empty public constructor
     */
    public MenuItemFragment() {}

    /**
     * Called when the fragment is created
     * @return Root view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_menu_item, container, false);

        //Initializes the UI
        circle = root.findViewById(R.id.circle);
        textView = root.findViewById(R.id.text_view);
        imageView = root.findViewById(R.id.image_view);

        initUI();

        return root;
    }

    public void setup(int drawableId, int stringId, int colorId, Runnable onClick) {
        this.drawableId = drawableId;
        this.stringId = stringId;
        this.colorId = colorId;
        this.onClickListener = (View view) -> onClick.run();

        initUI();
    }

    private void initUI() {
        try {
            circle.getDrawable().setColorFilter(getResources().getColor(colorId), PorterDuff.Mode.SRC_ATOP);
            imageView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), drawableId, getActivity().getTheme()));
            textView.setTextColor(getResources().getColor(colorId));
            textView.setText(getString(stringId));
            root.setOnClickListener(onClickListener);
            Log.i("String", getString(stringId));
        }
        catch (NullPointerException | Resources.NotFoundException ignored) {}
    }
}