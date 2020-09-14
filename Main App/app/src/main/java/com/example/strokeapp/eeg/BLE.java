package com.example.strokeapp.eeg;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.strokeapp.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"FieldCanBeLocal", "deprecation"})
public class BLE {

    private Activity activity;
    private Spinner spinner;
    private Button connect;

    private Map<String, BluetoothDevice> bluetoothDeviceMap = new HashMap<>();
    private List<String> bluetoothDevices = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private boolean connected = false;
    private boolean runThread = false;

    private final int REQUEST_ENABLE_BT = 1000;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;

    private final UUID SERIAL_UUID = UUID.fromString("0000dfb1-0000-1000-8000-00805f9b34fb");

    private boolean isScanning = false;
    private Handler handler;
    private final long MAX_SCAN_PERIOD = 7500;
    private final int MIN_SIGNAL_STRENGTH = -75;

    private String[] data = new String[] {};
    private Runnable dataAnalyzer;

    private BluetoothAdapter.LeScanCallback leScanCallback = (bluetoothDevice, signalStrength, bytes) -> {
        if (signalStrength >= MIN_SIGNAL_STRENGTH) {
            handler.post(() -> {
                String deviceString = String.format("%s; %s", bluetoothDevice.getName(), bluetoothDevice.getAddress());
                if (!bluetoothDeviceMap.containsKey(deviceString) && bluetoothDevice.getName() != null && bluetoothDevice.getAddress() != null) {
                    bluetoothDevices.add(deviceString);
                    bluetoothDeviceMap.put(deviceString,  bluetoothDevice);
                    spinnerAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    activity.runOnUiThread(() -> Toast.makeText(activity,"Bluetooth is off",Toast.LENGTH_SHORT).show());
                    break;
                case BluetoothAdapter.STATE_ON:
                    activity.runOnUiThread(() -> Toast.makeText(activity,"Bluetooth is on",Toast.LENGTH_SHORT).show());
                    break;
            }
        }
    };

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Connected to: " + device.getName(), Toast.LENGTH_SHORT).show());
                activity.runOnUiThread(() -> connect.setText(R.string.disconnect));
                gatt.discoverServices();
                connected = true;
                isScanning = false;
                scanLeDevice(false);
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                activity.runOnUiThread(() -> Toast.makeText(activity, "Device disconnected: " + device.getName(), Toast.LENGTH_SHORT).show());
                activity.runOnUiThread(() -> connect.setText(R.string.connect));
                connected = false;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
            for (BluetoothGattService bluetoothGattService: bluetoothGattServices) {
                Log.i(String.valueOf(bluetoothGattService.describeContents()), bluetoothGattService.getUuid().toString());

                for (BluetoothGattCharacteristic bluetoothGattCharacteristic: bluetoothGattService.getCharacteristics()) {
                    UUID uuid = bluetoothGattCharacteristic.getUuid();
                    if (uuid.equals(SERIAL_UUID)) {
                        gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                        gatt.writeCharacteristic(bluetoothGattCharacteristic);
                        runThread = true;
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (!runThread) {
                gatt.setCharacteristicNotification(characteristic, false);
                activity.runOnUiThread(() -> connect.setText(R.string.connect));
                return;
            }

            data = characteristic.getStringValue(0).split("\n");
            dataAnalyzer.run();
        }
    };

    public BLE(Activity activity, Spinner spinner, Button connect, Runnable dataAnalyzer) {
        this.activity = activity;
        this.spinner = spinner;
        this.connect = connect;
        this.dataAnalyzer = dataAnalyzer;

        spinnerAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, bluetoothDevices);
        spinner.setAdapter(spinnerAdapter);

        handler = new Handler(activity.getMainLooper());

        initiate();
    }

    private void initiate() {
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            activity.runOnUiThread(() -> Toast.makeText(activity,"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show());
        }

        activity.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            //Request for Bluetooth permissions
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableAdapter, REQUEST_ENABLE_BT);
        }
    }

    public void connect() {
        if (connected) {
            disconnect();
            return;
        }

        if (bluetoothDevices.size() == 0) {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Please connect a device", Toast.LENGTH_LONG).show());
            return;
        }

        activity.runOnUiThread(() -> connect.setText(R.string.connecting));

        device = bluetoothDeviceMap.get((String) spinner.getSelectedItem());
        if (device != null && device.getName() != null && device.getAddress() != null) {
            device.connectGatt(activity, true, bluetoothGattCallback);
        }
        else {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Please choose a valid device", Toast.LENGTH_LONG).show());
        }
    }

    public void disconnect() {
        if (isScanning) {
            scanLeDevice(false);
        }
        if (connected) {
            activity.runOnUiThread(() -> connect.setText(R.string.connect));
            runThread = false;
            connected = false;
        }
    }

    private void scanLeDevice(boolean enableScanning) {
        if (enableScanning && !isScanning) {
            bluetoothDevices.clear();
            bluetoothDeviceMap.clear();

            handler.postDelayed(() -> {
                if (isScanning) {
                    scanLeDevice(false);
                }
            }, MAX_SCAN_PERIOD);

            isScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        }
        else if (!enableScanning && isScanning) {
            bluetoothAdapter.stopLeScan((bluetoothDevice, i, bytes) -> {});
            isScanning = false;
        }
    }

    public void refresh() {
        scanLeDevice(!isScanning);
    }

    public String[] getData() {
        return data;
    }
}
