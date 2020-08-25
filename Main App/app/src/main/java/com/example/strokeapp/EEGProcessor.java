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
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

public class EEGProcessor {

    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String BT_MAC = "98:D3:31:F9:86:38";
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private Scanner scanner;
    private boolean deviceConnected = false;
    private boolean runThread;
    private Activity activity;
    private Button connectButton;
    private double[] rawData;
    private final int REQUEST_ENABLE_BT = 1000;

    private EEGBand[] eegBands;
    private FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
    private double[] frequencies, amplitudes;
    private double freqDiff;
    private final int sampleFreq = 256;
    private int fftLength;
    private boolean runRealTime;
    private int pointerPos = 0;

    public static EEGBand DELTA = new EEGBand(0f, 3.5f, "Delta");
    public static EEGBand THETA = new EEGBand(4f, 8f, "Theta");
    public static EEGBand ALPHA = new EEGBand(8f, 13f, "Alpha");
    public static EEGBand BETA = new EEGBand(13f, 30f, "Beta");

    public EEGProcessor(Activity activity, Button connectButton, EEGBand[] eegBands, boolean runRealTime) {
        this.activity = activity;
        this.connectButton = connectButton;
        this.connectButton.setOnClickListener((View view) -> connect());
        this.runRealTime = runRealTime;
        this.eegBands = eegBands;

        Arrays.sort(this.eegBands);
        if (runRealTime) {
            fftLength = 64;
            freqDiff = (double) sampleFreq / fftLength;
        }
    }

    private boolean BTinit() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(activity,"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
            return false;
        }

        if(!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableAdapter, REQUEST_ENABLE_BT);
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        device = bluetoothAdapter.getRemoteDevice(BT_MAC);
        return true;
    }

    private boolean BTconnect() {
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(BT_UUID);
            socket.connect();
            inputStream = socket.getInputStream();
            scanner = new Scanner(inputStream);
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

    private void connect() {
        if (deviceConnected) {
            runThread = false;
            try {
                scanner.close();
                socket.close();
                deviceConnected = false;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            connectButton.setText(R.string.connect);
        }
        else {
            new Thread(() -> {
                if (BTinit() && BTconnect()) {
                    deviceConnected = true;
                    beginListenForData();
                }
            }).start();
        }
    }

    private void beginListenForData() {
        runThread = true;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        while (!Thread.currentThread().isInterrupted() && runThread) {
            try {
                double sensorValue = Double.parseDouble(br.readLine());
                rawData[pointerPos] = sensorValue;
                pointerPos++;
                if (pointerPos == fftLength) {
                    processData(rawData);
                    calculateBandPowers();
                    pointerPos = 0;
                }
            }
            catch (IOException e) {
                break;
            }
        }
        activity.runOnUiThread(() -> Toast.makeText(activity, "Exiting thread", Toast.LENGTH_SHORT).show());
    }

    private void processData(double[] data) {
        Complex[] fftResults = fft.transform(data, TransformType.FORWARD);
        for (int i = 0; i < fftResults.length; i++) {
            Complex complex = fftResults[i];
            double a = complex.getReal(), b = complex.getImaginary();
            amplitudes[i] = Math.sqrt(a * a + b * b);
            frequencies[i] = (double) i * sampleFreq / fftLength;
        }
    }

    private void calculateBandPowers() {
        int eegBandCount = 0;
        for (int i = 0; i < amplitudes.length - 1; i++) {
            if (frequencies[i] < eegBands[eegBandCount].high) {
                Log.i(eegBands[eegBandCount].bandName, String.valueOf(eegBands[eegBandCount].val));
                eegBandCount++;
                if (eegBandCount == eegBands.length) {
                    break;
                }
            }
            eegBands[eegBandCount].val += (amplitudes[i] + amplitudes[i + 1]) * freqDiff / 2;
        }
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
