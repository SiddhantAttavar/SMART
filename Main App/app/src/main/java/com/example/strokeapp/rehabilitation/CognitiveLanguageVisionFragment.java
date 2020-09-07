package com.example.strokeapp.rehabilitation;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.strokeapp.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
@SuppressWarnings({"FieldCanBeLocal, UseCompatLoadingForDrawables", "unchecked", "DefaultLocale"})
public class CognitiveLanguageVisionFragment extends Fragment {

    private View root;
    private TextView question;
    private TextView timer;
    private Button submit, next;
    private Button[] options;

    private Random random = new Random();
    private CountDownTimer countDownTimer;

    private final long TIME = 20 * 1000;
    private final int OPTIONS = 4;
    private final int TOTAL_QUESTIONS = 2;
    private int score;
    private int questionNumber = 0;
    private int ans;
    private String strAns;
    private Map<String, Drawable>[] questions;

    public CognitiveLanguageVisionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        options = new Button[OPTIONS];

        root = inflater.inflate(R.layout.fragment_cognitive_language_vision, container, false);
        options[0] = root.findViewById(R.id.option_1);
        options[1] = root.findViewById(R.id.option_2);
        options[2] = root.findViewById(R.id.option_3);
        options[3] = root.findViewById(R.id.option_4);
        question = root.findViewById(R.id.question);
        timer = root.findViewById(R.id.timer);
        submit = root.findViewById(R.id.submit);
        next = root.findViewById(R.id.next);

        submit.setOnClickListener((View view) -> submitAnswer(questionNumber));
        next.setOnClickListener((View view) -> askQuestion());

        for (int i = 0; i < OPTIONS; i++) {
            int finalI = i;
            options[i].setOnClickListener((View view) -> submitAnswer(finalI));
        }

        setQuestions();

        return root;
    }

    private void setQuestions() {
        Resources resources = getResources();
        questions = new Map[] {new HashMap<>(), new HashMap<>()};

        String[] question1Options = resources.getStringArray(R.array.language_question_1_options);
        int[] question1Images = new int[] {
//                R.drawable.language_vision_q1_o1,
//                R.drawable.language_vision_q1_o2,
//                R.drawable.language_vision_q1_o3,
//                R.drawable.language_vision_q1_o4,
        };
        for (int i = 0; i < OPTIONS; i++) {
            questions[0].put(question1Options[i], resources.getDrawable(question1Images[i]));
        }

        String[] question2Options = resources.getStringArray(R.array.language_question_2_options);
        int[] question2Images = new int[] {
//                R.drawable.language_vision_q2_o1,
//                R.drawable.language_vision_q2_o2,
//                R.drawable.language_vision_q2_o3,
//                R.drawable.language_vision_q2_o4,
        };
        for (int i = 0; i < OPTIONS; i++) {
            questions[1].put(question2Options[i], resources.getDrawable(question2Images[i]));
        }
    }

    private void askQuestion() {
        root.setBackgroundColor(getResources().getColor(R.color.background));
        if (questionNumber < TOTAL_QUESTIONS) {
            ans = random.nextInt(OPTIONS);
            int i = 0;
            for (Map.Entry<String, Drawable> entry : questions[questionNumber].entrySet()) {
                options[i].setBackground(entry.getValue());
                if (i == ans) {
                    strAns = entry.getKey();
                }
                i++;
            }

            question.setText(String.format("%s %s", questionNumber, strAns));

            countDownTimer = new CountDownTimer(TIME, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timer.setText(String.valueOf((int) (millisUntilFinished / 1000)));
                }

                @Override
                public void onFinish() {
                    timer.setText(R.string.time_over);
                    submitAnswer(4);
                }
            };
            countDownTimer.start();
        }
        else {
            timer.setText(String.format("%d / %d", score, TOTAL_QUESTIONS));
        }
    }

    private void submitAnswer(int submission) {
        if (questionNumber < TOTAL_QUESTIONS) {
            if (submission == ans) {
                score++;
                root.setBackgroundColor(getResources().getColor(R.color.green));
            }
            else {
                root.setBackgroundColor(getResources().getColor(R.color.red));
            }
            countDownTimer.cancel();
            questionNumber++;
        }
    }
}