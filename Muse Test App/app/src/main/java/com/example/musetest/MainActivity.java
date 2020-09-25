package com.example.musetest;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings({"FieldCanBeLocal", "SourceLockedOrientationActivity", "DefaultLocale", "SetTextI18n", "PointlessArithmeticExpression"})
public class MainActivity extends AppCompatActivity {

    public MainActivity() {}

    private MuseFragment museFragment;
    private EEGFragment eegFragment;

    private final String EEG_FILE_NAME = "EEGlog.csv";
    private final String FFT_FILE_NAME = "FFTResults.csv";

    private int count = 0;

    private TextView eegTextView;
    private TextView museTextView;
    private TextView myEegTextView;
    private View ssvepView;

    private FileOutputStream eegFOS, fftFos;

    private long startTime;

    //We define the sampling frequency and the window length for real time analysis
    //and initialize the fftLength
    private final int samplingFreq = 220;
    private final int windowLengthTime = 4;
    private int fftLength;
    private double freqDiff = (double) 1 / windowLengthTime;
    private double[] rawData = new double[samplingFreq * windowLengthTime];
    private final int slidingWindowLength = 1 * samplingFreq;

    //We list the entire frequency band of interest (1 - 30 Hz) and a few common frequency bands in EEG-
    //Delta: 1 - 4 Hz; Theta: 4 - 8 Hz; Alpha: 8 - 14 Hz; Beta: 14 - 30 Hz
    public static final float LOW_PASS = 1, HIGH_PASS = 30;
    public static final float DELTA_LOW = 1, DELTA_HIGH = 4;
    public static final float THETA_LOW = 4, THETA_HIGH = 8;
    public static final float ALPHA_LOW = 8, ALPHA_HIGH = 14;
    public static final float BETA_LOW = 14, BETA_HIGH = 30;

    //We define an EEG Band for the total frequency band of interest
    private EEGFragment.EEGBand TOTAL = new EEGFragment.EEGBand(LOW_PASS, HIGH_PASS, "Total");

    EEGFragment.EEGBand[] eegBands = new EEGFragment.EEGBand[] {
            new EEGFragment.EEGBand(DELTA_LOW, DELTA_HIGH, "Delta"),
            new EEGFragment.EEGBand(THETA_LOW, THETA_HIGH, "Theta"),
            new EEGFragment.EEGBand(ALPHA_LOW, ALPHA_HIGH, "Alpha"),
            new EEGFragment.EEGBand(BETA_LOW, BETA_HIGH, "Beta")
    };

    EEGFragment.EEGBand[] eegBands2 = new EEGFragment.EEGBand[] {
            new EEGFragment.EEGBand(DELTA_LOW, DELTA_HIGH, "Delta"),
            new EEGFragment.EEGBand(THETA_LOW, THETA_HIGH, "Theta"),
            new EEGFragment.EEGBand(ALPHA_LOW, ALPHA_HIGH, "Alpha"),
            new EEGFragment.EEGBand(BETA_LOW, BETA_HIGH, "Beta")
    };

    //For storing of the frequencies and amplitudes obtained from the FFT results
    private double[] frequencies, amplitudes;

    //We use Apache Commons Math implementation of the FFT
    private FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);

    //SSVEP related variables
    private final int ssvepFrequency = 22;
    private final long ssvepTime = 1000 / 2 / ssvepFrequency;
    private boolean ssvepOn = false;
    private boolean ssvepRunning = false;
    private Thread thread;

    public MainActivity(EEGFragment eegFragment) {
        this.eegFragment = eegFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Initiate the UI
        ssvepView = findViewById(R.id.ssvep_view);
        eegTextView = findViewById(R.id.eeg_text_view);
        myEegTextView = findViewById(R.id.my_eeg_text_view);
        museTextView = findViewById(R.id.muse_text_view);

        eegFragment = (EEGFragment) getSupportFragmentManager().findFragmentById(R.id.eeg_fragment);
        assert eegFragment != null;
        eegFragment.setup(eegBands2, this::analyseResults);

        museFragment = (MuseFragment) getSupportFragmentManager().findFragmentById(R.id.muse_fragment);
        assert museFragment != null;
        museFragment.setDataAnalyzer(this::analyseData);

        try {
            eegFOS = openFileOutput(EEG_FILE_NAME, MODE_APPEND);
            fftFos = openFileOutput(FFT_FILE_NAME, MODE_APPEND);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        thread = new Thread(this::ssvepSetState);
        thread.start();

        startTime = System.currentTimeMillis();
    }

    private void ssvepSetState() {
        this.runOnUiThread(() -> {
            if (ssvepOn) {
                ssvepView.setBackgroundColor(Color.BLUE);
            }
            else {
                ssvepView.setBackgroundColor(Color.WHITE);
            }
        });
        ssvepOn = !ssvepOn;
        if (ssvepRunning) {
            try {
                Thread.sleep(ssvepTime);
                ssvepSetState();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * It is important to call stopListening when the Activity is paused
     * to avoid a resource leak from the LibMuse library.
     */
    protected void onPause() {
        super.onPause();
        museFragment.pause(false);
    }

    private void analyseData() {

        double eegVal = museFragment.getEeg();
        insertAtEnd(rawData, eegVal);
        count++;
        if (count == slidingWindowLength) {
            count = 0;
            new Thread(() -> processData(rawData)).start();
        }

        log(eegFOS, new double[] {eegVal});
    }

    private void log(FileOutputStream fileOutputStream, double[] msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((double) (System.currentTimeMillis() - startTime) / 1000);
        for (double val: msg) {
            stringBuilder.append(',').append(String.format("%.4f", val));
        }
        stringBuilder.append('\n');
        try {
            fileOutputStream.write(stringBuilder.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void insertAtEnd(double[] data, double value) {
        if (data.length - 1 >= 0) System.arraycopy(data, 1, data, 0, data.length - 1);
        data[data.length - 1] = value;
    }

    /**
     * Pads array to make its length same as FFT Length
     * @param data Array to pad
     * @param fftLength The length of array to return
     * @return Array of length FFT Length
     */
    private double[] zeroPadData(double[] data, int fftLength) {
        double[] finData = new double[fftLength];
        System.arraycopy(data, 0, finData, (fftLength - data.length) / 2, data.length);
        return finData;
    }

    /**
     * Processes the EEG Data using FFT to get the frequency and amplitudes
     * @param data The data to be processed
     */
    private void processData(double[] data) {
        fftLength = 1;
        while (fftLength < data.length) {
            fftLength *= 2;
        }
        if (data.length < fftLength) {
            //Zero pad the data if required to make it a power of 2 required by FFT
            data = zeroPadData(data, fftLength);
        }

        //We get FFT length number of frequency bins and amplitude values from the FFT
        frequencies = new double[fftLength];
        amplitudes = new double[fftLength];

        //Perform the FFT using Apache Commons Math library
        Complex[] fftResults = FFT.transform(data, TransformType.FORWARD);
        for (int i = 0; i < fftResults.length; i++) {
            Complex complex = fftResults[i];
            double real = complex.getReal(), imag = complex.getImaginary();

            //The frequency is given by the formula i * sampling frequency / FFT length
            //The amplitude corresponding to this frequency bin is given by
            //the Complex number (a + bi) returned in the FFT as sqrt(a * a + b * b)
            //i.e. the distance from the origin
            frequencies[i] = i * freqDiff;
            amplitudes[i] = Math.sqrt(real * real + imag * imag);

            log(fftFos, new double[] {frequencies[i], amplitudes[i]});

            if (frequencies[i] >= HIGH_PASS) {
                break;
            }
        }

        //Calculate the bandpowers of the frequency bands of interest
        calculateBandPowers();
    }

    /**
     * Calculates the Bandpowers for the frequency bands of interest
     * from the frequency and amplitude results
     * as well as the total bandpower of all frequencies which is used
     * for the calculate the relative bandpowers
     */
    private void calculateBandPowers() {
        int count = 0;

        //Reset all bandpower values to 0
        TOTAL.val = 0;
        for (EEGFragment.EEGBand eegBand: eegBands) {
            eegBand.val = 0;
        }

        //The values in the second half of the FFT results are
        //a mirror image of the first half
        for (int i = 1; i < fftLength; i++) {
            if (frequencies[i] > HIGH_PASS) {
                break;
            }

            if (eegBands[count].high < frequencies[i]) {

                //We need to move to the next frequency band or exit the loop
                if (count < eegBands.length - 1) {
                    count++;
                }
            }

            double val = amplitudes[i];
            if (eegBands[count].low <= frequencies[i]) {
                //This value comes under the current frequency band
                //We must add the value here to this band and the total value
                eegBands[count].val += val;
            }

            if (frequencies[i] >= LOW_PASS && frequencies[i] < HIGH_PASS) {
                TOTAL.val += val;
            }
        }
        analyseResults();
    }

    /**
     * Calculates the relative frequency of a frequency band
     * It is calulated as a the ratio of the
     * bandpower of the given band to the total bandpower
     * @param eegBand Frequency band of interest
     * @return Relative banpower
     */
    public double getRelativeBandpower(EEGFragment.EEGBand eegBand) {
        return eegBand.val / TOTAL.val;
    }

    private void analyseResults() {
        this.runOnUiThread(() -> {
            eegTextView.setText("My calculated Values - \n");
            for (EEGFragment.EEGBand eegBand : eegBands) {
                eegTextView.append(String.format("%s: %.4f\n", eegBand.bandName, getRelativeBandpower(eegBand)));
            }

            myEegTextView.setText("Colected EEG Values\n");
            for (EEGFragment.EEGBand eegBand: eegBands2) {
                myEegTextView.append(String.format("%s: %.4f\n", eegBand.bandName, eegFragment.getRelativeBandpower(eegBand)));
            }

            double sum = museFragment.delta + museFragment.theta + museFragment.alpha + museFragment.beta;

            museTextView.setText(String.format("Muse Values - \nDelta: %.4f\nTheta: %.4f\nAlpha: %.4f\nBeta: %.4f",
                    museFragment.delta / sum, museFragment.theta / sum, museFragment.alpha / sum, museFragment.beta / sum));
        });
    }

    public void setSSVEPRunning(View view) {
        ssvepRunning = !ssvepRunning;
        if (ssvepRunning) {
            thread = new Thread(this::ssvepSetState);
            thread.start();
        }
        else {
            try {
                thread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}