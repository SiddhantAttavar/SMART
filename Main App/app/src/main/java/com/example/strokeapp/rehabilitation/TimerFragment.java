package com.example.strokeapp.rehabilitation;

import android.os.Build;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.strokeapp.R;

@SuppressWarnings("FieldCanBeLocal")
public class TimerFragment extends Fragment {

    //UI elements
    private View root;
    private TextView timer;
    public Button next;
    private ProgressBar progressBar;

    //Whether the timer is completer
    private boolean done;

    //Countdown timer
    private CountDownTimer countDownTimer;

    /**
     * Required empty public constructor
     */
    public TimerFragment() {}

    /**
     * Called when the fragment is created
     * @return Root view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.fragment_timer, container, false);
        timer = root.findViewById(R.id.timer);
        next = root.findViewById(R.id.next);
        progressBar = root.findViewById(R.id.progress_bar);

        return root;
    }

    /**
     * Starts the timer
     * @param time time to run the timer for
     * @param onFinish Runnable to perform when the timer is completed
     * @param skippable Whether the timer is skippable
     */
    public void startTimer(long time, Runnable onFinish, boolean skippable) {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(time * 1000, 1000) {

            /**
             * Called every second and updates the UI with the new time
             * @param timeLeft Time left till end
             */
            @Override
            public void onTick(long timeLeft) {
                long timeDone = time - timeLeft;
                timer.setText(String.valueOf(timeDone / 1000));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progressBar.setProgress((int) timeDone, true);
                }
                else {
                    progressBar.setProgress((int) timeDone);
                }
            }

            /**
             * Called when the timer is completed
             */
            @Override
            public void onFinish() {
                if (!done) {
                    timer.setText(R.string.time_over);
                    onFinish.run();
                    done = true;
                    next.setOnClickListener((View view) -> {});
                }
            }
        };

        //Starts the timer
        countDownTimer.start();
        done = false;

        if (skippable) {
            //Sets the on click listener
            next.setOnClickListener((View view) -> {
                if (!done) {
                    done = true;
                    countDownTimer.cancel();
                    onFinish.run();
                }
            });
        }
    }

    /**
     * Cancels the timer and resets the UI
     */
    public void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timer.setText("");
    }

    /**
     * Sets the on click listener of the next button
     * @param onClickListener On click listener of the button to be set
     */
    public void setNextOnClickListener(View.OnClickListener onClickListener) {
        cancelTimer();
        next.setOnClickListener(onClickListener);
    }

    /**
     * Called when the fragment is paused or the linked activity has been exited
     * Cancels the timer
     */
    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}