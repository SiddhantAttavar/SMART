package com.example.strokeapp.rehabilitation;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.strokeapp.R;

import java.util.Random;

@SuppressWarnings("FieldCanBeLocal, DefaultLocale")
public class CognitiveMentalAptitudeFragment extends Fragment {

    private View root;
    private EditText answer;
    private TextView question;
    private TextView timer;
    private Button submit, next;

    private Random random = new Random();
    private CountDownTimer countDownTimer;

    private final int MAX_NUMBER = 9 + 1;
    private int x, y;
    private int ans;
    private char operation;
    private char[] operations = new char[] {'+', '-', '⨉'};

    private int score = 0;
    private int questions = 0;
    private final int TOTAL_QUESTIONS = 3;

    private final long TIME = 20 * 1000;

    public CognitiveMentalAptitudeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_cognitive_mental_aptitude, container, false);
        answer = root.findViewById(R.id.answer);
        question = root.findViewById(R.id.question);
        timer = root.findViewById(R.id.timer);
        submit = root.findViewById(R.id.submit);
        next = root.findViewById(R.id.next);

        submit.setOnClickListener((View view) -> submitAnswer());
        next.setOnClickListener((View view) -> askQuestion());

        askQuestion();
        return root;
    }

    private void askQuestion() {
        root.setBackgroundColor(getResources().getColor(R.color.background));
        if (questions < TOTAL_QUESTIONS) {
            answer.setText("");

            operation = operations[random.nextInt(3)];
            x = random.nextInt(MAX_NUMBER);
            if (operation == '-') {
                y = random.nextInt(x + 1);
            }
            else {
                y = random.nextInt(MAX_NUMBER);
            }

            if (operation == '+') {
                ans = x + y;
            }
            else if (operation == '-') {
                ans = x - y;
            }
            else {
                ans = x * y;
            }

            countDownTimer = new CountDownTimer(TIME, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timer.setText(String.valueOf((int) (millisUntilFinished / 1000)));
                }

                @Override
                public void onFinish() {
                    timer.setText(R.string.time_over);
                    submitAnswer();
                }
            };
            countDownTimer.start();

            question.setText(String.format("%d %s %d = ?", x, operation, y));
        }
        else {
            timer.setText(String.format("%d / %d", score, TOTAL_QUESTIONS));
        }
    }

    public void submitAnswer() {
        if (questions < TOTAL_QUESTIONS) {
            int submission = -1;
            if (!answer.getText().toString().equals("")) {
                submission = Integer.parseInt(answer.getText().toString());
            }
            if (submission == ans) {
                score++;
                root.setBackgroundColor(getResources().getColor(R.color.green));
            }
            else {
                root.setBackgroundColor(getResources().getColor(R.color.red));
            }
            questions++;
            countDownTimer.cancel();
        }
    }
}