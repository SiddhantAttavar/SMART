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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("FieldCanBeLocal, DefaultLocale")
public class CognitiveMemoryFragment extends Fragment {

    private View root;
    private TextView question;
    private EditText answer;
    private TextView timer;
    private Button submit, next;

    private String questionState = "Remembering Words";
    private boolean waitingForQuestion = false, askingQuestion = false;

    private Random random = new Random();
    private CountDownTimer countDownTimer;

    private final int TOTAL_WORDS = 3;
    private int score = 0;

    private final long REMEMBER_TIME = 15 * 1000;
    private final long WAIT_TIME = 20 * 1000;
    private final long QUESTION_TIME = 30 * 1000;

    private String[] words;
    private Set<String> wordSet;

    private boolean demo = false;

    public CognitiveMemoryFragment() {
        // Required empty public constructor
    }

    public CognitiveMemoryFragment(boolean demo) {
        this.demo = demo;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_cognitive_memory, container, false);
        answer = root.findViewById(R.id.answer);
        question = root.findViewById(R.id.question);
        timer = root.findViewById(R.id.timer);
        submit = root.findViewById(R.id.submit);
        next = root.findViewById(R.id.next);

        submit.setOnClickListener((View view) -> {
            if (questionState.equals("Asking question")) {
                submitAnswer();
            }
        });
        next.setOnClickListener((View view) -> {
            if (questionState.equals("Remembering Words") && !waitingForQuestion) {
                waitForQuestion();
            }
            else if (demo && questionState.equals("Waiting for question")) {
                askQuestion();
            }
        });

        String[] options = getResources().getStringArray(R.array.memory_words);
        wordSet = new HashSet<>();
        do {
            wordSet.add(options[random.nextInt(options.length)].toLowerCase());
        } while (wordSet.size() < TOTAL_WORDS);

        Object[] wordSetArray = wordSet.toArray();
        words = Arrays.copyOf(wordSetArray, wordSetArray.length, String[].class);

        StringBuilder temp = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            temp.append(", ").append(words[i]);
        }
        question.setText(getString(R.string.memory_question, REMEMBER_TIME / 1000, TOTAL_WORDS, temp.toString()));

        countDownTimer = new CountDownTimer(REMEMBER_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timer.setText(String.valueOf((int) (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                waitForQuestion();
            }
        };
        countDownTimer.start();

        return root;
    }

    private void waitForQuestion() {
        countDownTimer.cancel();
        waitingForQuestion = true;
        questionState=  "Waiting for question";

        question.setText(getString(R.string.memory_wait_instructions, WAIT_TIME / 1000));
        countDownTimer = new CountDownTimer(WAIT_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timer.setText(String.valueOf((int) (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                askQuestion();
            }
        };
        countDownTimer.start();
    }

    private void askQuestion() {
        countDownTimer.cancel();
        askingQuestion = true;
        questionState = "Asking question";

        question.setText(R.string.memory_instructions);
        answer.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(QUESTION_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timer.setText(String.valueOf((int) (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                if (askingQuestion) {
                    submitAnswer();
                }
            }
        };
        countDownTimer.start();
    }

    private void submitAnswer() {
        if (!askingQuestion) {
            return;
        }

        countDownTimer.cancel();
        askingQuestion = false;

        String[] submission = answer.getText().toString().split(",");
        for (int i = 0; i < submission.length; i++) {
            submission[i] = submission[i].trim().toLowerCase();
        }

        for (int i = 0; i < Math.min(TOTAL_WORDS, submission.length); i++) {
            if (wordSet.contains(submission[i])) {
                score++;
            }
        }

        if (Arrays.equals(submission, words)) {
            score += 2;
        }

        timer.setText(String.format("%d / %d", score, TOTAL_WORDS + 2));
    }
}