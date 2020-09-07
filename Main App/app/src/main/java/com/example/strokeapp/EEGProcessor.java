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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

@SuppressWarnings({"FieldCanBeLocal", "unused", "RedundantSuppression", "SpellCheckingInspection"})
public class EEGProcessor {

    //Bluetooth connectivity related variables
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String BT_MAC = "98:D3:31:F9:86:38";
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private boolean deviceConnected = false;

    private Thread thread;
    private boolean runThread;
    private Activity activity;
    private Button connectButton;
    private final int REQUEST_ENABLE_BT = 1000;
    private Runnable analyseResults;

    private EEGBand[] eegBands;
    private FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
    private double[] frequencies, amplitudes;
    private double freqDiff;
    private final int sampleFreq = 64;
    private final int samplingTime = 4;
    private int fftLength;
    private boolean runRealTime;

    public boolean test = false;

    public static final float LOW_PASS = 1f, HIGH_PASS = 30f;
    public static final float DELTA_LOW = 1f, DELTA_HIGH = 4f;
    public static final float THETA_LOW = 4f, THETA_HIGH = 8f;
    public static final float ALPHA_LOW = 8f, ALPHA_HIGH = 13f;
    public static final float BETA_LOW = 13f, BETA_HIGH = 30f;

    private EEGBand TOTAL = new EEGBand(LOW_PASS, HIGH_PASS, "Total");

    public EEGProcessor(Activity activity, Button connectButton, boolean runRealTime, Runnable analyseResults, EEGBand[] eegBands) {
        this.activity = activity;
        this.connectButton = connectButton;
        this.runRealTime = runRealTime;
        this.eegBands = eegBands;
        this.analyseResults = analyseResults;

        this.connectButton.setText(R.string.connect);
        this.connectButton.setOnClickListener((View view) -> connect());

        Arrays.sort(this.eegBands);

        if (runRealTime) {
            fftLength = sampleFreq * samplingTime;
            freqDiff = (double) sampleFreq / fftLength;
        }
    }

    private boolean BTinit() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            activity.runOnUiThread(() -> Toast.makeText(activity,"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show());
            return false;
        }

        if(!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableAdapter, REQUEST_ENABLE_BT);
        }

        device = bluetoothAdapter.getRemoteDevice(BT_MAC);
        return true;
    }

    private boolean BTconnect() {
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(BT_UUID);
            socket.connect();
            inputStream = socket.getInputStream();
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

    public void onPause() {
        if (deviceConnected) {
          runThread = false;
            try {
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
            });
            thread.start();
        }
    }

    private void listenForData() {
        runThread = true;
        try {
            Scanner scanner = new Scanner(activity.getAssets().open("EEG Sample Data.txt"));
            if (runRealTime) {
                double[] rawData = new double[fftLength];
                while (!Thread.currentThread().isInterrupted() && runThread) {
                    try {
                        double sensorValue = scanner.nextDouble();
                        insertAtEnd(rawData, sensorValue);
                        processData(rawData);
                        calculateBandPowers();
                        analyseResults.run();
                    }
                    catch (NoSuchElementException e) {
                        if (test) {
                            break;
                        }
                    }
                }
            }
            else {
                List<Double> rawData = new ArrayList<>();
                while (!Thread.currentThread().isInterrupted() && runThread) {
                    try {
                        double sensorValue = scanner.nextDouble();
                        rawData.add(sensorValue);
                    }
                    catch (NoSuchElementException e) {
                        if (test) {
                            break;
                        }
                    }
                }
                fftLength = 1;
                while (fftLength < rawData.size()) {
                    fftLength *= 2;
                }
                freqDiff = (double) sampleFreq / fftLength;
                double[] finalRawData = zeroPadData(getArr(rawData), fftLength);
                processData(finalRawData);
                calculateBandPowers();
                analyseResults.run();
            }
            scanner.close();
        }
        catch (IOException e) {
            Log.i("EEGProcessor", "File not found");
        }
    }

    private double[] getArr(List<Double> list) {
        double[] temp = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            temp[i] = list.get(i);
        }
        return temp;
    }

    private void processData(double[] data) {
        if (data.length < fftLength) {
            data = zeroPadData(data, fftLength);
        }
        frequencies = new double[fftLength];
        amplitudes = new double[fftLength];
        Complex[] fftResults = fft.transform(data, TransformType.FORWARD);
        for (int i = 0; i < fftResults.length; i++) {
            Complex complex = fftResults[i];
            double a = complex.getReal(), b = complex.getImaginary();
            amplitudes[i] = Math.sqrt(a * a + b * b);
            frequencies[i] = i * freqDiff;
        }
    }

    private void calculateBandPowers() {
        int count = 0;
        TOTAL.val = 0;
        for (EEGBand eegBand: eegBands) {
            eegBand.val = 0;
        }
        for (int i = 0; i < fftLength / 2; i++) {
            if (eegBands[count].high < frequencies[i]) {
                count++;
                if (count == eegBands.length) {
                    break;
                }
            }
            if (eegBands[count].low <= frequencies[i]) {
                eegBands[count].val += amplitudes[i];
                TOTAL.val += amplitudes[i];
            }
        }
    }

    private double[] zeroPadData(double[] data, int fftLength) {
        double[] finData = new double[fftLength];
        System.arraycopy(data, 0, finData, (fftLength - data.length) / 2, data.length);
        return finData;
    }

    private void insertAtEnd(double[] array, double value) {
        System.arraycopy(array, 1, array, 0, array.length - 1);
        array[array.length - 1] = value;
    }

    public double getRelativeBandpower(EEGBand eegBand) {
        return eegBand.val / TOTAL.val;
    }

    public static class EEGBand implements Comparable<EEGBand> {
        public float low, high;
        public double val = 0;
        public String bandName;

        public EEGBand(float low, float high, String bandName) {
            this.low = low;
            this.high = high;
            this.bandName = bandName;
        }

        @Override
        public int compareTo(EEGBand eegBand) {
            return (int) (this.high - eegBand.high + this.low - eegBand.low);
        }
    }
}
