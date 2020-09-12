package com.example.strokeapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression", "SpellCheckingInspection", "ResultOfMethodCallIgnored"})
public class EEGProcessor {

    //Bluetooth connectivity related variables
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String BT_MAC = "98:D3:31:F9:86:38";
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private final int REQUEST_ENABLE_BT = 1000;

    //Limit for the EEG Buffer's and whether we want buffered data for smoother results
    private final int BUFFER_LIMIT = 16;
    private boolean bufferedData;

    //Whether the device is connected or not
    private boolean deviceConnected = false;

    //We create a new Thread for the EEG data acquistion and processing 
    //to avoid blocking other processes going on in the main thread
    private Thread thread;
    private boolean runThread;
    
    //The activity using this class for passing to other functions
    private Activity activity;
    
    //A button for connecting/disconneccting to the EEG headset
    private Button connectButton;

    //Whether we want to do real time analysis or not
    private boolean runRealTime;
    
    //This Runnable is called whenever we want to analyse our data
    private Runnable analyseResults;
    
    //We use Apache Commons Math implementation of the FFT
    private FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);
    
    //For storing of the frequencies and amplitudes obtained from the FFT results
    private double[] frequencies, amplitudes;
    
    //We define the sampling frequency and the window length for real time analysis
    //and initialize the fftLength
    private final int samplingFreq = 64;
    private final int windowLengthTime = 4;
    private int fftLength;

    //The frequency readings are spaced out at 1 / window length or sampling frequency / FFT length intervals
    private double freqDiff;

    //Whether we want to test the analysis with sample EEG data in /assets/EEG Sample Data.txt
    public boolean test = false;

    //The EEGBands that we want to monitor
    //Note: The frequency bands must not be overlapping
    private EEGBand[] eegBands;
    
    //We list the entire frequency band of interest (1 - 30 Hz) and a few common frequency bands in EEG-
    //Delta: 1 - 4 Hz; Theta: 4 - 8 Hz; Alpha: 8 - 14 Hz; Beta: 14 - 30 Hz
    public static final float LOW_PASS = 1f, HIGH_PASS = 30f;
    public static final float DELTA_LOW = 1f, DELTA_HIGH = 4f;
    public static final float THETA_LOW = 4f, THETA_HIGH = 8f;
    public static final float ALPHA_LOW = 8f, ALPHA_HIGH = 14f;
    public static final float BETA_LOW = 14f, BETA_HIGH = 30f;

    //We define an EEG Band for the total frequency band of interest
    private EEGBand TOTAL = new EEGBand(LOW_PASS, HIGH_PASS, "Total");

    /**
     * Constructor for EEG Processor
     * @param activity Acitivty that is using the EEG Processor
     * @param connectButton Button that is used for connect/disconnect functionality
     * @param runRealTime Whether we want to run real time analysis
     * @param analyseResults Runnable to be called after analysis
     * @param eegBands EEG frequency bands of interest
     */
    public EEGProcessor(Activity activity, EEGBand[] eegBands, Button connectButton, boolean runRealTime, boolean bufferedData, Runnable analyseResults) {
        this.activity = activity;
        this.connectButton = connectButton;
        this.runRealTime = runRealTime;
        this.eegBands = eegBands;
        this.analyseResults = analyseResults;
        this.bufferedData = bufferedData;

        //Set an on click listener for the connect/disconnect button
        this.connectButton.setText(R.string.connect);
        this.connectButton.setOnClickListener((View view) -> connect());

        //Sort the eegBands for future use
        Arrays.sort(this.eegBands);

        if (runRealTime) {
            //Define the FFT length and frequency diff
            fftLength = samplingFreq * windowLengthTime;
            freqDiff = (double) samplingFreq / fftLength;
        }
    }

    /**
     * Initialise the EEG Headset via Bluetooth and save the device
     * @return Whether the initialisation is succesful
     */
    private boolean BTinit() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            //The device doesn't  support Bluetooth
            activity.runOnUiThread(() -> Toast.makeText(activity,"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show());
            return false;
        }

        if(!bluetoothAdapter.isEnabled()) {
            //Request for Bluetooth persmissions
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableAdapter, REQUEST_ENABLE_BT);
        }

        //Save the device
        device = bluetoothAdapter.getRemoteDevice(BT_MAC);
        return true;
    }

    /**
     * Connec to the device and get the input stream
     * @return Whether the connection is succesful
     */
    private boolean BTconnect() {
        try {
            socket = device.createRfcommSocketToServiceRecord(BT_UUID);
            socket.connect();

            //Get the input stream
            if (test) {
                inputStream = activity.getApplicationContext().getAssets().open("EEG Sample Data.txt");
            }
            else {
                inputStream = socket.getInputStream();
            }

            //Display a Toast to notify the user that the device has been connected
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "Device connected", Toast.LENGTH_SHORT).show();
                activity.runOnUiThread(() -> connectButton.setText(R.string.disconnect));
            });

            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Called when the user exits the activity screen
     */
    public void onPause() {
        if (deviceConnected) {
            runThread = false;
            try {
                //Close the connection
                if (socket != null) {
                    socket.close();
                }
                deviceConnected = false;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Change the running state
     */
    public void setRunRealTime(boolean runRealTime) {
        this.runThread = runRealTime;
    }

    /**
     * Called when the user clicks on the connect/disconnect button
     * If the device is connected it stops the thread
     * Else it creates a new Thread and starts it to begin listening for data
     */
    private void connect() {
        if (deviceConnected) {
            runThread = false;
            try {
                thread.join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            connectButton.setText(R.string.connect);
        }
        else {
            thread = new Thread(() -> {
                if (test || (BTinit() && BTconnect())) {
                    deviceConnected = true;
                    listenForData();
                }
                else {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Device not connected. Please try again", Toast.LENGTH_SHORT).show());
                }
            });
            thread.start();
        }
    }

    /**
     * Recieves new data and starts analysis if necessary
     */
    private void listenForData() {
        runThread = true;

        //We use an array for real time analysis since it has a fixed length
        //For analysis at the end we use an ArrayList since it can have a variable length
        double[] rawData = new double[fftLength];
        List<Double> rawDataList = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        while (!Thread.currentThread().isInterrupted() && runThread) {
            try {
                if (bufferedReader.ready()) {
                    try {
                        double sensorValue = Double.parseDouble(bufferedReader.readLine());

                        //Add data to the end
                        if (runRealTime) {
                            insertAtEnd(rawData, sensorValue);

                            //Perform processing of data
                            processData(rawData);
                        }
                        else {
                            rawDataList.add(sensorValue);
                        }
                    }
                    catch (NumberFormatException ignored) {}
                }
                else {
                    Log.i("", "No data");
                }
            }
            catch (IOException e) {
                runThread = false;
                if (test) {
                    //We are reading from the EEG Sample Data.txt file and have reached the end
                    //so we need to exit the loop
                    break;
                }
            }
        }

        if (!runRealTime) {
            //Calculate the length of the FFT and the frequency difference
            fftLength = 1;
            while (fftLength < rawDataList.size()) {
                fftLength *= 2;
            }
            freqDiff = (double) samplingFreq / fftLength;

            //Perform processing of data
            processData(getArr(rawDataList));
        }

        try {
            inputStream.close();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts a List to an array
     * @param list List to convert to array
     * @return Array
     */
    private double[] getArr(List<Double> list) {
        double[] temp = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            temp[i] = list.get(i);
        }
        return temp;
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
     * Processes the EEG Data using FFT to get the frequnecy and ampliudes
     * @param data The data to be processed
     */
    private void processData(double[] data) {
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
        }

        //Calculate the bandpowers of the frquency bands of interest
        calculateBandPowers();
    }

    /**
     * Calulates the Bandpowers for the frequency bands of interest
     * from the frequency and amplitude results
     * as well as the total bandpoewr of all frquencies which is used
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
        for (int i = 0; i < fftLength / 2; i++) {
            if (eegBands[count].high < frequencies[i]) {
                if (bufferedData) {
                    //Add the value to the buffer
                    eegBands[count].buffer.add(eegBands[count].val);
                }

                //We need to move to the next frequency band or exit the loop
                count++;
                if (count == eegBands.length) {
                    break;
                }
            }
            if (eegBands[count].low <= frequencies[i]) {
                //This value comes under the current frequency band
                //We must add the value here to this band and the total value
                eegBands[count].val += amplitudes[i];
            }
            if (TOTAL.low <= frequencies[i] && TOTAL.high > frequencies[i]) {
                TOTAL.val += amplitudes[i];
            }
        }

        //We perform the analysis only if the buffer is full
        if (bufferedData && eegBands[0].buffer.size() == BUFFER_LIMIT) {
            //We average the buffer readings to get smoother results
            for (EEGBand eegBand: eegBands) {
                eegBand.val = 0;
                for (double bufferValue: eegBand.buffer) {
                    eegBand.val += bufferValue;
                }
                eegBand.val /= BUFFER_LIMIT;
            }

            //Perform analysis
            analyseResults.run();

            //Reset the buffers
            for (EEGBand eegBand: eegBands) {
                eegBand.buffer = new ArrayList<>();
            }
        }
        else if (!runRealTime) {
            analyseResults.run();
        }
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
         * Used in sorting the bands in order of low frequencies to highest frquencies
         * @param eegBand Frequency band to be compared to
         * @return Integer representing whether it has lower higher or equal frequencies
         */
        @Override
        public int compareTo(EEGBand eegBand) {
            return (int) (this.high - eegBand.high + this.low - eegBand.low);
        }
    }
}
