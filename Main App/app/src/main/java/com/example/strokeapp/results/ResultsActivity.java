package com.example.strokeapp.results;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.strokeapp.R;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.Stack;

@SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression"})
@SuppressLint("DefaultLocale")
public class ResultsActivity extends AppCompatActivity {

    //UI elements
    private LinearLayout linearLayout;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private LinearLayout.LayoutParams layoutParams;

    //Constant ids for the tests and rehabilitation types
    public static final int TESTS = 0;
    public static final int REHABILITATION = 1;

    //File IO (Input/Output) related variables
    private static final String ID_FILE_NAME = "id.csv";
    private static final String FILE_NAME = "results.csv";
    private static FileOutputStream fileOutputStream;
    private FileInputStream fileInputStream;
    private Scanner scanner;

    /**
     * Called when the activity is created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        //Instantiates UI
        linearLayout = findViewById(R.id.linear_layout);
        fragmentManager = getSupportFragmentManager();

        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(16, 8, 16, 8);

        //Reads file
        readFile();
    }

    /**
     * Reads the FILE_NAME (results.csv) file
     * and adds each part to the linear layout
     */
    private void readFile() {
        try {
            fileInputStream = openFileInput(FILE_NAME);
            scanner = new Scanner(fileInputStream);

            fragmentTransaction = getSupportFragmentManager().beginTransaction();

            //Adds the data being read from the file so they can be accessed in the reverse order
            Stack<String[]> stack = new Stack<>();
            while (scanner.hasNext()) {
                String[] data = scanner.nextLine().split(",");
                if (data.length != 4) {
                    continue;
                }
                stack.add(data);
            }

            //Adds the various views to the layout
            while (!stack.empty()) {
                String[] data = stack.pop();
                long id = Long.parseLong(data[0]);
                String type = data[1];
                String name = data[2];
                String result = data[3];

                if (type.equals("Tests")) {
                    addView(TESTS, id, name, result);
                }
                else {
                    addView(REHABILITATION, id, name, result);
                }
            }

            fragmentTransaction.commit();

            //Close the input related variables
            scanner.close();
            fileInputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a view to the linear layout
     * @param type Type of event (TEST/REHABILITATION)
     * @param id Id of event
     * @param name Name of event
     * @param result Result of event
     */
    private void addView(int type, long id, String name, String result) {
        ResultsFragment resultsFragment = new ResultsFragment();
        resultsFragment.setup(type, name, id, result);

        fragmentTransaction.add(R.id.linear_layout, resultsFragment, String.format("Card %d", id));
    }

    /**
     * Returns the available Id
     * @param context Context from which this function is called
     * @return Available id
     */
    private static long getId(Context context) {
        long id = 1;
        try {
            //Reads the last available id from ID_FILE_NAME (id.csv)
            Scanner idScanner = new Scanner(context.openFileInput(ID_FILE_NAME));
            id = idScanner.nextLong();
            idScanner.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //Increments the value in ID_FIEL_NAME (id.csv)
            FileOutputStream idFileOutputStream = context.openFileOutput(ID_FILE_NAME, MODE_PRIVATE);
            idFileOutputStream.write(String.valueOf(id + 1).getBytes());
            idFileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return id;
    }

    /**
     * Logs an event in FILE_NAME (results.csv)
     * @param context Context from which this function is being called
     * @param type Type of event (TEST/REHABILITATION)
     * @param name Name of event
     * @param result Result of event
     */
    public static void log(Context context, int type, String name, String result) {
        if (fileOutputStream == null) {
            try {
                fileOutputStream = context.openFileOutput(FILE_NAME, MODE_APPEND);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        long id = getId(context);

        //CSV cells are separated by commas and rows are separated by new lines
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(id).append(',');
        if (type == TESTS) {
            stringBuilder.append("Tests");
        }
        else {
            stringBuilder.append("Rehabilitation");
        }
        stringBuilder.append(',').append(name).append(',').append(result).append('\n');
        try {
            fileOutputStream.write(stringBuilder.toString().getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}