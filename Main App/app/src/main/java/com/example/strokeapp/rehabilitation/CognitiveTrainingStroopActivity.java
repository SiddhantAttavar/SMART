package com.example.strokeapp.rehabilitation;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.R;
import com.example.strokeapp.results.ResultsActivity;

import java.util.Random;

@SuppressWarnings("FieldCanBeLocal")
public class CognitiveTrainingStroopActivity extends AppCompatActivity {

    //UI elements
    private LinearLayout root;
    private TableLayout tableLayout;
    private TextView instructions;
    private TextView question;
    private View[] colorOptions;
    private TimerFragment timerFragment;

    //Background of the root and question
    private Drawable background, questionBackground;

    //Arrays containing the question options
    private String[] options;
    private int[] colors = new int[] {
            R.color.green,
            R.color.red,
            R.color.yellow,
            R.color.blue
    };

    //Random number generator
    private Random random = new Random();

    //Test related variables
    private int score = 0;
    private int questions = 0;
    private int ans;
    private int submission;
    private boolean askingColorQuestion = false;

    //Constants: No. of questions, no. of options, time per question
    private final int TOTAL_QUESTIONS = 3;
    private final int TOTAL_OPTIONS = 4;
    private final int QUESTION_TIME = 20;

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cognitive_training_stroop);

        //Initialize UI
        root = findViewById(R.id.root);
        tableLayout = findViewById(R.id.table_layout);
        instructions = findViewById(R.id.instructions);
        question = findViewById(R.id.question);
        timerFragment = (TimerFragment) getSupportFragmentManager().findFragmentById(R.id.timer_fragment);

        colorOptions = new View[TOTAL_OPTIONS];
        colorOptions[0] = findViewById(R.id.option_1);
        colorOptions[1] = findViewById(R.id.option_2);
        colorOptions[2] = findViewById(R.id.option_3);
        colorOptions[3] = findViewById(R.id.option_4);

        for (int i = 0; i < TOTAL_OPTIONS; i++) {
            int finalI = i;
            colorOptions[i].setOnClickListener((View view) -> onClick(finalI));
        }

        background = root.getBackground();
        questionBackground = question.getBackground();

        options = getResources().getStringArray(R.array.stroop_test_options);

        //Begin the test
        askQuestion();
    }

    /**
     * Asks the question to the user
     * Selects a random word and a color not equal to the word
     * and displays it to the user
     */
    private void askQuestion() {
        if (questions > TOTAL_QUESTIONS) {
            return;
        }

        root.setBackground(background);

        //Select random word
        ans = random.nextInt(TOTAL_OPTIONS);

        //Select a color not equal to the word
        int colorOption;
        do {
            colorOption = random.nextInt(TOTAL_OPTIONS);
        } while (colorOption == ans);


        question.setText(options[ans]);
        question.setBackgroundColor(getResources().getColor(colors[colorOption]));

        //Ask the question
        if (questions < TOTAL_QUESTIONS) {
            instructions.setText(R.string.cognitive_stroop_training_instructions_color);
            timerFragment.startTimer(QUESTION_TIME, this::endQuestion, true);
            tableLayout.setVisibility(View.VISIBLE);
            submission = -1;
            askingColorQuestion = true;
        }

        questions++;
    }

    /**
     * Ends the question and displays the result on the screen
     */
    private void endQuestion() {
        if (submission == -1 || ans != submission) {
            root.setBackgroundColor(getResources().getColor(R.color.red));
        }
        else {
            root.setBackgroundColor(getResources().getColor(R.color.green));
            score++;
        }

        instructions.setText(getString(R.string.cognitive_training_score, score, questions));

        tableLayout.setVisibility(View.INVISIBLE);
        question.setText("");
        question.setBackground(questionBackground);
        timerFragment.cancelTimer();

        if (questions < TOTAL_QUESTIONS) {
            timerFragment.setNextOnClickListener(this::askQuestion);
        }
        else {
            instructions.append("\nThis test is complete");
            ResultsActivity.log(this, ResultsActivity.REHABILITATION, getString(R.string.cognitive_stroop_training), getString(R.string.cognitive_training_score, score,  TOTAL_QUESTIONS));
        }
    }

    /**
     * Called when a button is clicked
     * @param submission Button which is clicked
     */
    private void onClick(int submission) {
        if (askingColorQuestion) {
            this.submission = submission;
        }
        endQuestion();
    }
}