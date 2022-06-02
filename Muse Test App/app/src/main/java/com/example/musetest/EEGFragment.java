package com.example.musetest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression", "PointlessArithmeticExpression", "ConstantConditions"})
public class EEGFragment extends Fragment {

    //UI elements
    private View root;
    private Button refresh, connect;
    private Spinner spinner;
    private Activity activity;

    //Bluetooth Low Energy Manager class
    private BLE ble;

    //Data analyzer
    private Runnable analyseResults;

    //Limit for the EEG Buffer's and whether we want buffered data for smoother results
    private final int BUFFER_LIMIT = 32;

    //We define the sampling frequency and the window length for real time analysis
    //and initialize the fftLength
    private final int samplingFreq = 256;
    private final int windowLengthTime = 4;
    private int fftLength;
    private double freqDiff = (double) 1 / windowLengthTime;
    private double[] rawData = new double[samplingFreq * windowLengthTime];
    private final int slidingWindowLength = 1 * samplingFreq;

    //For storing of the frequencies and amplitudes obtained from the FFT results
    private double[] frequencies, amplitudes;

    //The EEGBands that we want to monitor
    //Note: The frequency bands must not be overlapping
    private EEGBand[] eegBands;

    private int count = 0;

    //We list the entire frequency band of interest (1 - 30 Hz) and a few common frequency bands in EEG-
    //Delta: 1 - 4 Hz; Theta: 4 - 8 Hz; Alpha: 8 - 14 Hz; Beta: 14 - 30 Hz
    public static final float LOW_PASS = 1f, HIGH_PASS = 30f;
    public static final float DELTA_LOW = 1f, DELTA_HIGH = 3f;
    public static final float THETA_LOW = 3f, THETA_HIGH = 7f;
    public static final float ALPHA_LOW = 7f, ALPHA_HIGH = 13f;
    public static final float BETA_LOW = 13f, BETA_HIGH = 30f;

    //We define an EEG Band for the total frequency band of interest
    private EEGBand TOTAL = new EEGBand(LOW_PASS, HIGH_PASS, "Total");

    //We create a new Thread for the EEG data acquisition and processing
    //to avoid blocking other processes going on in the main thread
    private Thread thread;
    private boolean runThread;

    //We use Apache Commons Math implementation of the FFT
    private FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);

    //Time at which we start capturing data
    private long startTime;

    //Files for logging data
    private final String RAW_EEG_DATA_FILE_NAME = "RawEEGData.csv";
    private final String FFT_RESULTS_FILE_NAME = "FFTResults.csv";
    private final String BANDPOWER_RESULTS_FILE_NAME = "BandpowerResults.csv";

    private FileOutputStream rawEEGDataFileOutputStream;
    private FileOutputStream fftResultsFileOutputStream;
    private FileOutputStream bandpowerResultsFileOutputStream;

    private Runnable dataAnalyzer = () -> {
        for (String data: ble.getData()) {
            try {
                double eegVal = Double.parseDouble(data);

                //Log the data in the logging mechanism and file
                Log.i("Data", String.valueOf(eegVal));
                /*log(rawEEGDataFileOutputStream, new double[] {
                        (double) System.currentTimeMillis(),
                        eegVal
                });*/

                insertAtEnd(rawData, eegVal);
                count++;
                if (count == slidingWindowLength) {
                    count = 0;
                    new Thread(() -> processData(rawData)).start();
                }
            }
            catch (NumberFormatException ignored) {}
        }
    };

    /**
     * Required empty public constructor
     */
    public EEGFragment() {}

    /**
     * Called when the fragment is created
     * @return the root view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        activity = requireActivity();
        root = inflater.inflate(R.layout.fragment_eeg, container, false);
        refresh = root.findViewById(R.id.refresh);
        connect = root.findViewById(R.id.connect);
        spinner = root.findViewById(R.id.spinner);

        //Instantiate the BLE manager class
        ble = new BLE(activity, spinner, refresh, connect, dataAnalyzer);
        startTime = System.currentTimeMillis();

        //Setup files and file output streams
        try {
            rawEEGDataFileOutputStream = getContext().openFileOutput(RAW_EEG_DATA_FILE_NAME, Context.MODE_APPEND);
            fftResultsFileOutputStream = getContext().openFileOutput(FFT_RESULTS_FILE_NAME, Context.MODE_APPEND);
            bandpowerResultsFileOutputStream = getContext().openFileOutput(BANDPOWER_RESULTS_FILE_NAME, Context.MODE_APPEND);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return root;
    }

    /**
     * Sets up the class
     * @param eegBands EEG frequency bands of interest
     * @param analyseResults Runnable to analyse reults
     */
    public void setup(EEGBand[] eegBands, Runnable analyseResults) {
        this.eegBands = eegBands;
        this.analyseResults = analyseResults;

        //Sort the eegBands for future use
        Arrays.sort(this.eegBands);
    }

    /**
     * Called when the fragment is paused
     * Disconnects BLE device
     */
    @Override
    public void onPause() {
        super.onPause();
        ble.disconnect();
    }

    /**
     * Called when we want to disconnect the device
     * Performs analysis if we do not wish to run real time
     */
    public void disconnect() {
        ble.disconnect();
    }

    /**
     * Returns whether the device is connected
     * @return Whether the device is connected
     */
    public boolean isConnected() {
        return ble.connected;
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
     * Inserts a value at the end of an array and removes the first value
     * @param array Initial array
     * @param value Value to append
     */
    private void insertAtEnd(double[] array, double value) {
        System.arraycopy(array, 1, array, 0, array.length - 1);
        array[array.length - 1] = value;
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
        double time = (double) System.currentTimeMillis();

        for (int i = 0; i < fftResults.length; i++) {
            Complex complex = fftResults[i];
            double real = complex.getReal(), imag = complex.getImaginary();

            //The frequency is given by the formula i * sampling frequency / FFT length
            //The amplitude corresponding to this frequency bin is given by
            //the Complex number (a + bi) returned in the FFT as sqrt(a * a + b * b)
            //i.e. the distance from the origin
            frequencies[i] = i * freqDiff;
            amplitudes[i] = Math.sqrt(real * real + imag * imag);

            //Log the results
            /*log(fftResultsFileOutputStream, new double[] {
                    time,
                    frequencies[i],
                    amplitudes[i]
            });*/

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
        for (EEGBand eegBand: eegBands) {
            eegBand.val = 0;
        }

        //The values in the second half of the FFT results are
        //a mirror image of the first half
        for (int i = 1; i < fftLength; i++) {
            if (frequencies[i] > HIGH_PASS) {
                break;
            }

            if (count < eegBands.length && eegBands[count].high < frequencies[i]) {
                //We need to move to the next frequency band or exit the loop
                count++;
            }

            double val = amplitudes[i];
            if (count < eegBands.length && eegBands[count].low <= frequencies[i]) {
                //This value comes under the current frequency band
                //We must add the value here to this band and the total value
                eegBands[count].val += val;
            }

            if (frequencies[i] >= LOW_PASS && frequencies[i] < HIGH_PASS) {
                TOTAL.val += val;
            }
        }

        double[] data = new double[eegBands.length + 1];
        data[0] = (double) System.currentTimeMillis();
        for (int i = 0; i < eegBands.length; i++) {
            data[i + 1] = eegBands[i].val;
        }
        log(bandpowerResultsFileOutputStream, data);

        analyseResults.run();
    }

    private void log(FileOutputStream fileOutputStream, double[] msg) {
        StringBuilder stringBuilder = new StringBuilder(String.valueOf(msg[0]));
        for (int i = 1; i < msg.length; i++) {
            stringBuilder.append(',').append(msg[i]);
        }
        stringBuilder.append('\n');

        try {
            fileOutputStream.write(stringBuilder.toString().getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public double[] getFFTResults() {
        return amplitudes;
    }

    public double[] getFrequencies() {
        return frequencies;
    }

    public double[] getRawData() {
        return rawData;
    }

    /**
     * Calculates the relative frequency of a frequency band
     * It is calulated as a the ratio of the
     * bandpower of the given band to the total bandpower
     * @param eegBand Frequency band of interest
     * @return Relative banpower
     */
    public double getRelativeBandpower(EEGBand eegBand) {
        return eegBand.val / TOTAL.val;
    }

    /**
     * Class for defining a frequency band
     * We can define a frequency band as all the frequencies in a given range
     */
    public static class EEGBand implements Comparable<EEGBand> {
        //Basic properties of the band: min frequency, max frequency, bandpower, name and
        //buffer for storing temporary EEG values
        public float low, high;
        public double val = 0;
        public String bandName;
        public List<Double> buffer = new ArrayList<>();

        /**
         * Constructor for the band
         * Defines the band using min frequency, max frequency and band name
         * @param low Minimum frequency of interest
         * @param high Maximum frequency of interest
         * @param bandName Name of the band
         */
        public EEGBand(float low, float high, String bandName) {
            this.low = low;
            this.high = high;
            this.bandName = bandName;
        }

        /**
         * Compares this frequency band to another given band
         * Used in sorting the bands in order of low frequencies to highest frequencies
         * @param eegBand Frequency band to be compared to
         * @return Integer representing whether it has lower higher or equal frequencies
         */
        @Override
        public int compareTo(EEGBand eegBand) {
            return (int) (this.high - eegBand.high + this.low - eegBand.low);
        }
    }
}