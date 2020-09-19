package com.example.strokeapp.rehabilitation;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.R;
import com.example.strokeapp.results.ResultsActivity;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression", "UseCompatLoadingForDrawables"})
public class CognitiveTrainingNumberActivity extends AppCompatActivity {

    //UI elements
    private TimerFragment timerFragment;
    private TextView instructions;
    private TableLayout tableLayout;
    private TableRow[] gridRows;

    //Grid of the game cells
    private GameNumber[][] gameGrid;

    //Layout options for the table rows and buttons
    private TableLayout.LayoutParams rowLayoutParams;
    private TableRow.LayoutParams buttonLayoutParams;

    //The button background
    private Drawable buttonBackground;

    //Game related variables
    private int totalNumbers = 3;
    private int gridSize = 3;
    private Set<Integer> gameNumbers;

    //Random number generator
    private Random random = new Random();

    //Time related variables
    //Time for remembering: 20 seconds
    //Time for answering question: 20 seconds
    private final int REMEMBER_TIME = 20;
    private final int QUESTION_TIME = 20;


    //Score related variables
    private int MAX_SCORE = 0;
    private int currentValue;
    private int score;
    private int totalScore = 0;

    //Question related variables
    private boolean order;
    private boolean askingQuestion = false;
    private int questions = 0;
    private final int TOTAL_QUESTIONS = 3;

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cognitive_training_number);

        //Instantiates the UI
        instructions = findViewById(R.id.instructions);
        tableLayout = findViewById(R.id.game_grid);
        timerFragment = (TimerFragment) getSupportFragmentManager().findFragmentById(R.id.timer_fragment);

        rowLayoutParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, 0);
        rowLayoutParams.weight = 1;

        buttonLayoutParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
        buttonLayoutParams.weight = 1;
        buttonLayoutParams.setMargins(2, 2, 2, 2);

        //Starts the game with 3 numbers and a 3 * 3 grid
        startGame(3, 3);
    }

    /**
     * Called when a button on the game grid is clicked
     * @param row Row of the button clicked
     * @param col Column of the button clicked
     */
    private void onClick(int row, int col) {
        if (!askingQuestion) {
            return;
        }

        if (gameNumbers.contains(row * gridSize + col)) {
            if (gameGrid[row][col].val == currentValue) {
                gameGrid[row][col].button.setBackgroundColor(getResources().getColor(R.color.green));
            }
            else {
                order = false;
                gameGrid[row][col].button.setBackgroundColor(getResources().getColor(R.color.yellow));
            }

            //Increase the score
            score++;
        }
        else {
            gameGrid[row][col].button.setBackgroundColor(getResources().getColor(R.color.red));
            order = false;
        }

        currentValue++;

        if (currentValue > totalNumbers) {
            //We have attempted all numbers and we have the end the question
            timerFragment.cancelTimer();
            endQuestion();
        }
    }

    /**
     * Start the game
     * @param totalNumbers Numbers to start the game with
     * @param gridSize Grid size (It is a square)
     */
    private void startGame(int totalNumbers, int gridSize) {
        if (questions > TOTAL_QUESTIONS) {
            return;
        }

        this.totalNumbers = totalNumbers;
        this.gridSize = gridSize;

        setUpGrid();
        setGamePosition();

        //Initialize the timer
        timerFragment.startTimer(REMEMBER_TIME, this::askQuestion, true);
    }

    /**
     * Asks the question to the user
     */
    private void askQuestion() {
        askingQuestion = true;
        instructions.setText(R.string.cognitive_number_training_question);

        for (Integer position: gameNumbers) {
            //Hide the button
            int row = position / gridSize;
            int col = position % gridSize;
            gameGrid[row][col].setShown(false);
            gameGrid[row][col].button.setBackground(buttonBackground);
        }

        currentValue = 1;
        order = true;
        score = 0;

        //Start the timer for the question to end
        timerFragment.startTimer(QUESTION_TIME, this::endQuestion, true);
    }

    /**
     * Ends the question
     */
    private void endQuestion() {
        if (questions >= TOTAL_QUESTIONS) {
            return;
        }

        askingQuestion = false;
        tableLayout.removeAllViews();

        if (order && currentValue > totalNumbers) {
            //The user has clicked all buttons in order so we increase the score by 2
            score += 2;
        }

        //Increase the total score and the maximum score
        totalScore += score;
        MAX_SCORE += totalNumbers + 2;

        instructions.setText(getString(R.string.cognitive_training_score, totalScore, MAX_SCORE));

        questions++;

        if (questions < TOTAL_QUESTIONS) {
            //Increment the grid size and the total numbers to be asked for
            gridSize++;
            totalNumbers++;
            timerFragment.setNextOnClickListener(() -> startGame(totalNumbers, gridSize));
        }
        else {
            instructions.append("\nThis test is complete");
            ResultsActivity.log(this, ResultsActivity.REHABILITATION, "Cognitive Number Test", getString(R.string.cognitive_training_score, totalScore, MAX_SCORE));
        }
    }

    /**
     * Sets up the grid
     */
    private void setUpGrid() {
        //Instantiate the game grid and the grid rows
        gameGrid = new GameNumber[gridSize][gridSize];
        gridRows = new TableRow[gridSize];

        for (int row = 0; row < gridSize; row++) {
            //Create a row, add buttons to them and add it to the table layout
            gridRows[row] = new TableRow(this);

            for (int col = 0; col < gridSize; col++) {
                //Create the button and add it to the table row
                Button button = new Button(this);
                int finalRow = row, finalCol = col;
                button.setOnClickListener((View view) -> onClick(finalRow, finalCol));
                gameGrid[row][col] = new GameNumber(row, col, button);
                gridRows[row].addView(button, buttonLayoutParams);
                if (buttonBackground == null) {
                    buttonBackground = button.getBackground();
                }
            }

            tableLayout.addView(gridRows[row], rowLayoutParams);
        }

        instructions.setText(R.string.cognitive_number_training_instructions);
    }

    /**
     * Sets up the game positions
     * Selects random numbers and add them to a set
     */
    private void setGamePosition() {
        for (GameNumber[] gameRow: gameGrid) {
            for (GameNumber gameNumber: gameRow) {
                gameNumber.setShown(false);
                gameNumber.setVal(-1);
            }
        }

        gameNumbers = new HashSet<>();

        while (gameNumbers.size() < totalNumbers) {
            int position = random.nextInt(gridSize * gridSize);

            if (gameNumbers.add(position)) {
                //The row is the position / grid size
                //The column is the position % grid size
                int col = position % gridSize;
                int row = position / gridSize;

                gameGrid[row][col].setShown(true);
                gameGrid[row][col].setVal(gameNumbers.size());
            }
        }
    }

    /**
     * Game Number class for each cell in the game grid
     */
    private static class GameNumber {

        //Variables defining the class: position, value, button, and whether the button is shown
        private boolean isShown = false;
        private int x, y;
        private int val;
        private Button button;

        /**
         * Constructor for the Game number
         * @param x Column of the game number
         * @param y Row of the game number
         * @param button Button linked to the game number
         */
        public GameNumber(int x, int y, Button button) {
            this.x = x;
            this.y = y;
            this.button = button;
        }

        /**
         * Makes the game number visible/invisible
         * @param shown Whether the game number should be visible
         */
        public void setShown(boolean shown) {
            isShown = shown;

            if (isShown) {
                button.setText(String.valueOf(val));
            }
            else {
                button.setText("");
            }
        }

        /**
         * Sets the value of the game number
         * @param val Value of the game number to be set
         */
        public void setVal(int val) {
            this.val = val;
            if (isShown) {
                button.setText(String.valueOf(val));
            }
        }
    }
}