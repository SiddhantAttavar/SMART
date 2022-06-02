package com.example.musetest;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MuseFragment extends Fragment {

    private Spinner musesSpinner;
    private ArrayAdapter<String> spinnerAdapter;
    private Button connectButton;

    private MuseManagerAndroid museManager;
    private Muse muse;
    private ConnectionListener connectionListener;
    private DataListener dataListener;

    private boolean isConnected = false;

    //For logging purposes
    private final String TAG = "Stroke App";

    private Runnable dataAnalyzer;

    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     *
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     *
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     **/
    private final double[] EEGBuffer = new double[6];
    public double eeg;

    private final double[] alphaBuffer = new double[6];
    public double alpha;

    private final double[] betaBuffer = new double[6];
    public double beta;

    private final double[] thetaBuffer = new double[6];
    public double theta;

    private final double[] deltaBuffer = new double[6];
    public double delta;


    public MuseFragment() {
        // Required empty public constructor
    }

    //---------------------------------------
    //Lifetime callbacks

    /**
     * Instantiate different parts of the Project
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        museManager = MuseManagerAndroid.getInstance();
        museManager.setContext(getContext());

        // Register a listener to receive connection, muse and data state changes.
        WeakReference<MuseFragment> weakActivity = new WeakReference<>(MuseFragment.this);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);
        museManager.setMuseListener(new MuseL(weakActivity));
    }

    /**
     * It is important to call stopListening when the Activity is paused
     * to avoid a resource leak from the LibMuse library.
     */
    public void pause(boolean isListening) {
        if (isListening) {
            super.onPause();
            museManager.stopListening();
        }
        else {
            // We need to set the context on MuseManagerAndroid before we can do anything.
            // This must come before other LibMuse API calls as it also loads the library.
            museManager = MuseManagerAndroid.getInstance();
            museManager.setContext(getContext());

            // Register a listener to receive connection, muse and data state changes.
            WeakReference<MuseFragment> weakActivity = new WeakReference<>(MuseFragment.this);
            connectionListener = new ConnectionListener(weakActivity);
            dataListener = new DataListener(weakActivity);
            museManager.setMuseListener(new MuseL(weakActivity));

            // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
            // simplify the connection process.  This requires access to the COARSE_LOCATION
            // or FINE_LOCATION permissions.  Make sure we have these permissions before
            // proceeding.
            ensurePermissions();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        //UI Elements
        View root = inflater.inflate(R.layout.fragment_muse, container, false);

        //Initialize UI
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        musesSpinner = root.findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
        connectButton = root.findViewById(R.id.connect);
        Button refresh = root.findViewById(R.id.refresh);

        connectButton.setOnClickListener((View view) -> connect());
        refresh.setOnClickListener((View view) -> refresh());

        return root;
    }

    /**
     * Start listening for nearby or paired Muse headbands. We call stopListening
     * first to make sure startListening will clear the list of headbands and start fresh.
     */
    public void refresh() {
        museManager.stopListening();
        museManager.startListening();
    }

    /**
     * The user has pressed the "Connect" button to connect to
     * the headband in the spinner.
     * Listening is an expensive operation, so now that we know
     * which headband the user wants to connect to we can stop
     * listening for other headbands.
     */
    public void connect() {
        //Check if we are connected or not
        if (isConnected) {
            if (muse != null) {
                muse.disconnect();
            }

            //Set the button text to connect
            connectButton.setText(R.string.connect);
            isConnected = false;
        }
        else {
            //We can stop listeneing for Muses
            museManager.stopListening();
            List<Muse> availableMuses = museManager.getMuses();

            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
                Toast.makeText(getContext(), "There is nothing to connect to", Toast.LENGTH_SHORT).show();
            }
            else {
                // Cache the Muse that the user has selected.
                muse = availableMuses.get(musesSpinner.getSelectedItemPosition());
                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.
                //The data we want is
                muse.unregisterAllListeners();
                muse.registerConnectionListener(connectionListener);
                muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.BETA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.THETA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_RELATIVE);

                // Initiate a connection to the headband and stream the data asynchronously.
                muse.runAsynchronously();

                //Set the button text to disconnect
                connectButton.setText(R.string.disconnect);
                isConnected = true;
            }
        }
    }

    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     *
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.
            DialogInterface.OnClickListener buttonListener = (dialog, which) -> {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                    };

            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_description)
                    .setPositiveButton(R.string.permission_dialog_understand, buttonListener)
                    .create();
            introDialog.show();
        }
    }

    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time a headband is discovered.
     * In this example, we update the spinner with the MAC address of the headband.
     */
    public void museListChanged() {
        final List<Muse> list = museManager.getMuses();
        spinnerAdapter.clear();
        for (Muse m : list) {
            spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     * @param p     A packet containing the current and prior connection states
     * @param muse  The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        final String status = p.getPreviousConnectionState() + " -> " + current;
        Log.i(TAG, status);

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // Save the data file once streaming has stopped.
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;
        }
    }

    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     */
    public void receiveMuseDataPacket(final MuseDataPacket p) {
        // valuesSize returns the number of data values contained in the packet.
        switch (p.packetType()) {
            case EEG:
                eeg = getEegChannelValues(EEGBuffer, p);
                Log.i("EEG", String.format("%.4f", eeg));
                dataAnalyzer.run();
                break;

            case ALPHA_RELATIVE:
                alpha = getEegChannelValues(alphaBuffer, p);
                Log.i(TAG, "Alpha: " + alpha + "; Beta: " + beta);
                break;

            case BETA_RELATIVE:
                beta = getEegChannelValues(betaBuffer, p);
                break;

            case THETA_RELATIVE:
                theta = getEegChannelValues(thetaBuffer, p);
                break;

            case DELTA_RELATIVE:
                delta = getEegChannelValues(deltaBuffer, p);
                break;
        }
    }

    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     *
     * getEEGChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */
    private double getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
        double sum = 0;
        int totalCount = 0;
        for (double i: buffer) {
            if (!Double.isNaN(i)) {
                sum += i;
                totalCount++;
            }
        }
        return sum / totalCount;
    }

    public void setDataAnalyzer(Runnable dataAnalyzer) {
        this.dataAnalyzer = dataAnalyzer;
    }

    public double getEeg() {
        return eeg;
    }

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.

    /**
     * Checks for changes in available Muse headsets
     */
    static class MuseL extends MuseListener {
        final WeakReference<MuseFragment> activityRef;

        MuseL(final WeakReference<MuseFragment> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }
    }

    /**
     * Check for changes in Muse connection status
     */
    static class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MuseFragment> activityRef;

        ConnectionListener(final WeakReference<MuseFragment> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    /**
     * Checks for incoming data
     */
    static class DataListener extends MuseDataListener {
        final WeakReference<MuseFragment> activityRef;

        DataListener(final WeakReference<MuseFragment> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p);
        }

        @Override
        public void receiveMuseArtifactPacket(MuseArtifactPacket museArtifactPacket, Muse muse) {
            //Leave blank
        }
    }
}
