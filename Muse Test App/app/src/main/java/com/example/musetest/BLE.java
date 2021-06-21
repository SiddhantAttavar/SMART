package com.example.musetest;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"FieldCanBeLocal", "SuspiciousMethodCalls"})
public class BLE {

    //UI elements
    private Activity activity;
    private Spinner spinner;
    private Button connect;

    //Data structures for storing and accessing the bluetooth device
    private Map<String, BluetoothDevice> bluetoothDeviceMap = new HashMap<>();
    private List<String> bluetoothDevices = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    //Variables to store the state of the connection and processing
    public boolean connected = false;
    private boolean runThread = false;

    //Permission code
    private final int REQUEST_ENABLE_BT = 1000;

    //Variables for managing the available devices and storing the required one
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothGatt bluetoothGatt;

    //The Serial port UUID of Bluno is 0000dfb1-0000-1000-8000-00805f9b34fb
    private final UUID SERIAL_UUID = UUID.fromString("0000dfb1-0000-1000-8000-00805f9b34fb");

    //Bluetooth Low Energy scanning related variables
    private boolean isScanning = false;
    private Handler handler;
    private final long MAX_SCAN_PERIOD = 7500;
    private final int MIN_SIGNAL_STRENGTH = -75;

    //Data storage and analysis related variables
    private String[] data = new String[] {};
    private Runnable dataAnalyzer;

    /**
     * Called when a new BLE device is discovered by the phone
     */
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

    /**
     * Called when there is a change in Bluetooth connectivity state and notifies the user
     */
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

    /**
     * BLE Gatt Callback overrides the functions:
     * onConnectionStateChange, onServicesDiscovered, onCharacteristicChanged
     */
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        /**
         * Called when the phone has connected/disconnected with a BLE device
         * @param gatt Bluetooth Gatt
         * @param newState New state of the connection
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //The device has connected with the phone
                //We notify the user and then discover the various services of the device
                activity.runOnUiThread(() -> Toast.makeText(activity, "Connected to: " + device.getName(), Toast.LENGTH_SHORT).show());
                activity.runOnUiThread(() -> connect.setText(R.string.disconnect));
                Log.i("Discovering Services", String.valueOf(gatt.discoverServices()));
                connected = true;
                isScanning = false;
                scanLeDevice(false);
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //The device has disconnected with the phone
                //We notify the user and then stop all background processes
                activity.runOnUiThread(() -> Toast.makeText(activity, "Device disconnected: " + device.getName(), Toast.LENGTH_SHORT).show());
                activity.runOnUiThread(() -> connect.setText(R.string.connect));
                connected = false;
            }
        }

        /**
         * Called when a BlE service os discovered
         * @param gatt Bluetooth Gatt
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i("BLE", "Services Discovered");
            List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
            for (BluetoothGattService bluetoothGattService: bluetoothGattServices) {
                Log.i(String.valueOf(bluetoothGattService.describeContents()), bluetoothGattService.getUuid().toString());

                for (BluetoothGattCharacteristic bluetoothGattCharacteristic: bluetoothGattService.getCharacteristics()) {
                    UUID uuid = bluetoothGattCharacteristic.getUuid();
                    Log.i("BLE", "Debug");
                    Log.i("UUID", uuid.toString());
                    if (uuid.equals(SERIAL_UUID)) {
                        //We have found the required BLE characteristic and registered it
                        gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                        gatt.writeCharacteristic(bluetoothGattCharacteristic);
                        runThread = true;
                    }
                }
            }
        }

        /**
         * Called when the characterstics changes (new data has been received)
         * @param gatt Bluetooth Gatt
         * @param characteristic Characteristic involved
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (!runThread) {
                gatt.setCharacteristicNotification(characteristic, false);
                activity.runOnUiThread(() -> connect.setText(R.string.connect));
                return;
            }

            //Get the data and perform analysis
            data = characteristic.getStringValue(0).split("\n");
            dataAnalyzer.run();
        }
    };

    /**
     * Constructor for the BLE Class
     * @param activity Activity instantiating the class
     * @param spinner Spinner to display available devices
     * @param connect Button to connect/disconnect from device
     * @param dataAnalyzer Runnable which performs analysis on the data
     */
    public BLE(Activity activity, Spinner spinner, Button refresh, Button connect, Runnable dataAnalyzer) {
        this.activity = activity;
        this.spinner = spinner;
        this.connect = connect;
        this.dataAnalyzer = dataAnalyzer;

        //Connect the spinner to the bluetoothDevices Adapter and set the on click listeners
        spinnerAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, bluetoothDevices);
        spinner.setAdapter(spinnerAdapter);

        refresh.setOnClickListener(view -> scanLeDevice(true));
        connect.setOnClickListener(view -> connect());

        //Instantiate the Handler
        handler = new Handler(activity.getMainLooper());

        //Check if BLE is available
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Device doesn't Support Bluetooth", Toast.LENGTH_SHORT).show());
        }

        //Register the broadcast receiver
        activity.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        //Get the bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            //Request for Bluetooth permissions
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableAdapter, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Connects to the device
     */
    public void connect() {
        if (connected) {
            disconnect();
            return;
        }

        if (bluetoothDevices.size() == 0) {
            //No devices available to connect to
            activity.runOnUiThread(() -> Toast.makeText(activity, "Please connect a device", Toast.LENGTH_LONG).show());
            return;
        }

        activity.runOnUiThread(() -> connect.setText(R.string.connecting));

        //Get the device and connect to it
        device = bluetoothDeviceMap.get(spinner.getSelectedItem());
        if (device != null && device.getName() != null && device.getAddress() != null) {
            device.connectGatt(activity, true, bluetoothGattCallback);
        }
        else {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Please choose a valid device", Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Disconnects the device or stops BLE scan
     */
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

    /**
     * Starts/Stop scanning
     * @param enableScanning Whether to start or stop the BLE scan
     */
    private void scanLeDevice(boolean enableScanning) {
        if (enableScanning && !isScanning) {
            //Clear the spinner and the device map
            bluetoothDevices.clear();
            bluetoothDeviceMap.clear();

            //Stop scanning after MAX_SCAN_PERIOD
            handler.postDelayed(() -> {
                if (isScanning) {
                    scanLeDevice(false);
                }
            }, MAX_SCAN_PERIOD);

            isScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        }
        else if (!enableScanning && isScanning) {
            //Stop scan
            bluetoothAdapter.stopLeScan((bluetoothDevice, i, bytes) -> {});
            isScanning = false;
        }
    }

    /**
     * Returns the data as a String[] of all data missed since the last check
     * @return Data to return
     */
    public String[] getData() {
        return data;
    }
}
