package com.example.musetest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.choosemuse.libmuse.Accelerometer;
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
import com.choosemuse.libmuse.MuseVersion;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String FILE_NAME = "EEGlog.csv";
    private final float STROKE_THRESHOLD = 3.7f;

    private TextView[] accelText = new TextView[3];
    private TextView[] EEGText = new TextView[4];
    private TextView[] alphaText = new TextView[4];
    private TextView[] betaText = new TextView[4];
    private TextView[] thetaText = new TextView[4];
    private TextView[] deltaText = new TextView[4];
    private TextView[] DARText = new TextView[4];
    private TextView[] DTRText = new TextView[4];
    private TextView result;
    private EditText editText;
    private Spinner musesSpinner;

    /*
    private TextView acc_x, acc_y, acc_z;
    private TextView tp9, fp1, fp2, tp10;
    private TextView alphaelem1, alphaelem2, alphaelem3, alphaelem4;
    private TextView betaelem1, betaelem2, betaelem3, betaelem4;
    private TextView thetaelem1, thetaelem2, thetaelem3, thetaelem4;
    private TextView deltaelem1, deltaelem2, deltaelem3, deltaelem4;
    private TextView darelem1, darelem2, darelem3, darelem4;
    private TextView dtrelem1, dtrelem2, dtrelem3, dtrelem4;
    */

    private DecimalFormat df = new DecimalFormat("#.##");

    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "MuseTest";

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     *
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     *
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

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
     */
    private final double[] EEGBuffer = new double[6];
    private boolean EEGStale;
    private double EEG;

    private final double[] alphaBuffer = new double[6];
    private boolean alphaStale;
    private double alpha;

    private final double[] betaBuffer = new double[6];
    private boolean betaStale;
    private double beta;

    private final double[] thetaBuffer = new double[6];
    private boolean thetaStale;
    private double theta;

    private final double[] deltaBuffer = new double[6];
    private boolean deltaStale;
    private double delta;

    private final double[] DTRBuffer = new double[6];
    private boolean DTRStale;
    private double DTR;

    private final double[] DARBuffer = new double[6];
    private boolean DARStale;
    private double DAR;

    private final double[] accelBuffer = new double[3];
    private boolean accelStale;

    private List<EEGData> dataSessionList = new ArrayList<>();
    private int id;

    private List<EEGData> data;
    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Handler handler = new Handler();

    /**
     * In the UI, the list of Muses you can connect to is displayed in a Spinner object for this example.
     * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
     */
    private ArrayAdapter<String> spinnerAdapter;

    /**
     * It is possible to pause the data transmission from the headband.  This boolean tracks whether
     * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
     */
    private boolean dataTransmission = true;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Initiate the UI
        initUI();

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        WeakReference<MainActivity> weakActivity = new WeakReference<>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);
        // Register a listener to receive notifications of what Muse headbands
        // we can connect to.
        manager.setMuseListener(new MuseL(weakActivity));

        // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
        // simplify the connection process.  This requires access to the COARSE_LOCATION
        // or FINE_LOCATION permissions.  Make sure we have these permissions before
        // proceeding.
        ensurePermissions();

        data = readData();
        if(data == null) {
            id = 0;
        }
        else {
            id = data.get(data.size() - 1).id + 1;
        }

        // Start our asynchronous updates of the UI.
        handler.post(tickUi);
    }

    /**
     * It is important to call stopListening when the Activity is paused
     * to avoid a resource leak from the LibMuse library.
     */
    protected void onPause() {
        super.onPause();
        manager.stopListening();
    }

    /**
     * Start listening for nearby or paired Muse headbands. We call stopListening
     * first to make sure startListening will clear the list of headbands and start fresh.
     */
    public void refresh(View view) {
        manager.stopListening();
        manager.startListening();
    }

    /**
     * The user has pressed the "Connect" button to connect to
     * the headband in the spinner.
     * Listening is an expensive operation, so now that we know
     * which headband the user wants to connect to we can stop
     * listening for other headbands.
     */
    public void connect(View view) {
        manager.stopListening();

        List<Muse> availableMuses = manager.getMuses();

        // Check that we actually have something to connect to.
        if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
            Log.w(TAG, "There is nothing to connect to");
        }
        else {

            // Cache the Muse that the user has selected.
            muse = availableMuses.get(musesSpinner.getSelectedItemPosition());
            // Unregister all prior listeners and register our data listener to
            // receive the MuseDataPacketTypes we are interested in.  If you do
            // not register a listener for a particular data type, you will not
            // receive data packets of that type.
            //The data we want is
            muse.unregisterAllListeners();
            muse.registerConnectionListener(connectionListener);
            muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
            muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_ABSOLUTE);
            muse.registerDataListener(dataListener, MuseDataPacketType.BETA_ABSOLUTE);
            muse.registerDataListener(dataListener, MuseDataPacketType.THETA_ABSOLUTE);
            muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_ABSOLUTE);
            muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
            muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
            muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);

            // Initiate a connection to the headband and stream the data asynchronously.
            muse.runAsynchronously();
        }
    }

    /**
     * The user has pressed the "Disconnect" button.
     * Disconnect from the selected Muse.
     */
    public void disconnect(View view) {
        if (muse != null) {
            muse.disconnect();
        }
    }

    /**
     * The user has pressed the "Pause/Resume" button to either pause or
     * resume data transmission.  Toggle the state and pause or resume the
     * transmission on the headband.
     */
    public void pauseOrResume(View view) {
        if (muse != null) {
            dataTransmission = !dataTransmission;
            muse.enableDataTransmission(dataTransmission);
        }
    }

    //--------------------------------------
    // Permissions

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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.

            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which){
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                        }
                    };

            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(this)
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
        final List<Muse> list = manager.getMuses();
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

        // Update the UI with the change in connection state.
        handler.post(new Runnable() {
            @Override
            public void run() {

                final TextView statusText = findViewById(R.id.con_status);
                statusText.setText(status);

                final MuseVersion museVersion = muse.getMuseVersion();
                final TextView museVersionText = findViewById(R.id.version);
                // If we haven't yet connected to the headband, the version information
                // will be null.  You have to connect to the headband before either the
                // MuseVersion or MuseConfiguration information is known.
                if (museVersion != null) {
                    final String version = museVersion.getFirmwareType() + " - "
                            + museVersion.getFirmwareVersion() + " - "
                            + museVersion.getProtocolVersion();
                    museVersionText.setText(version);
                } else {
                    museVersionText.setText(R.string.undefined);
                }
            }
        });

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
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                if (BuildConfig.DEBUG && !(EEGBuffer.length >= n)) {
                    throw new AssertionError("Buffer too small");
                }
                EEG = getEegChannelValues(EEGBuffer, p);
                EEGStale = true;
                break;

            case ACCELEROMETER:
                if (BuildConfig.DEBUG && !(accelBuffer.length >= n)) {
                    throw new AssertionError("Buffer too small");
                }
                getAccelValues(accelBuffer, p);
                accelStale = true;
                break;

            case ALPHA_ABSOLUTE:
                if (BuildConfig.DEBUG && !(alphaBuffer.length >= n)) {
                    throw new AssertionError("Buffer too small");
                }
                alpha = getEegChannelValues(alphaBuffer, p);
                alphaStale = true;

                //Check if we need to uodate DAR buffer
                if (!Arrays.equals(deltaBuffer, new double[6])) {
                    DAR = getDARRatios(DARBuffer);
                    DARStale = true;
                }
                break;

            case BETA_ABSOLUTE:
                if (BuildConfig.DEBUG && !(betaBuffer.length >= n)) {
                    throw new AssertionError("Buffer too small");
                }
                beta = getEegChannelValues(betaBuffer, p);
                betaStale = true;
                break;

            case THETA_ABSOLUTE:
                if (BuildConfig.DEBUG && !(thetaBuffer.length >= n)) {
                    throw new AssertionError("Buffer too small");
                }
                theta = getEegChannelValues(thetaBuffer, p);
                thetaStale = true;
                if (!Arrays.equals(deltaBuffer, new double[6])) {
                    DTR = getDTRRatios(DTRBuffer);
                    DTRStale = true;
                }
                break;

            case DELTA_ABSOLUTE:
                if (BuildConfig.DEBUG && !(deltaBuffer.length >= n)) {
                    throw new AssertionError("Buffer too small");
                }
                delta = getEegChannelValues(deltaBuffer, p);
                deltaStale = true;
                break;

            case BATTERY:
                //Should display warning when battery is low
            case DRL_REF:
                //Should use this data to eliminate noise
            default:
                break;
        }
    }

    private double getDARRatios(double[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = deltaBuffer[i] / alphaBuffer[i];
        }
        double sum = 0;
        for (double i: buffer) {
            sum += i;
        }
        return sum / buffer.length;
    }

    private double getDTRRatios(double[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = deltaBuffer[i] / thetaBuffer[i];
        }
        double sum = 0;
        for (double i: buffer) {
            sum += i;
        }
        return sum / buffer.length;
    }

    /**
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     * @param p     The artifact packet with the data from the headband.
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
        //To be developed further
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
        for (double i: buffer) {
            sum += i;
        }
        return sum / buffer.length;
    }

    private void getAccelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getAccelerometerValue(Accelerometer.X);
        buffer[1] = p.getAccelerometerValue(Accelerometer.Y);
        buffer[2] = p.getAccelerometerValue(Accelerometer.Z);
    }

    /**
     * Initializes the UI of the example application.
     */
    private void initUI() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        musesSpinner = findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);

        accelText[0] = findViewById(R.id.acc_x);
        accelText[1] = findViewById(R.id.acc_y);
        accelText[2] = findViewById(R.id.acc_z);

        EEGText[0] = findViewById(R.id.eeg_tp9);
        EEGText[0] = findViewById(R.id.eeg_af7);
        EEGText[0] = findViewById(R.id.eeg_af8);
        EEGText[0] = findViewById(R.id.eeg_tp10);

        alphaText[0] = findViewById(R.id.alpha_elem1);
        alphaText[1] = findViewById(R.id.alpha_elem2);
        alphaText[2] = findViewById(R.id.alpha_elem3);
        alphaText[3] = findViewById(R.id.alpha_elem4);

        betaText[0] = findViewById(R.id.beta_elem1);
        betaText[1] = findViewById(R.id.beta_elem2);
        betaText[2] = findViewById(R.id.beta_elem3);
        betaText[3] = findViewById(R.id.beta_elem4);

        thetaText[0] = findViewById(R.id.theta_elem1);
        thetaText[1] = findViewById(R.id.theta_elem2);
        thetaText[2] = findViewById(R.id.theta_elem3);
        thetaText[3] = findViewById(R.id.theta_elem4);

        deltaText[0] = findViewById(R.id.delta_elem1);
        deltaText[1] = findViewById(R.id.delta_elem2);
        deltaText[2] = findViewById(R.id.delta_elem3);
        deltaText[3] = findViewById(R.id.delta_elem4);

        DARText[0] = findViewById(R.id.dar_elem1);
        DARText[1] = findViewById(R.id.dar_elem2);
        DARText[2] = findViewById(R.id.dar_elem3);
        DARText[3] = findViewById(R.id.dar_elem4);

        DTRText[0] = findViewById(R.id.dtr_elem1);
        DTRText[1] = findViewById(R.id.dtr_elem2);
        DTRText[2] = findViewById(R.id.dtr_elem3);
        DTRText[3] = findViewById(R.id.dtr_elem4);

        /*
        acc_x = findViewById(R.id.acc_x);
        acc_y = findViewById(R.id.acc_y);
        acc_z = findViewById(R.id.acc_z);

        tp9 = findViewById(R.id.eeg_tp9);
        fp1 = findViewById(R.id.eeg_af7);
        fp2 = findViewById(R.id.eeg_af8);
        tp10 = findViewById(R.id.eeg_tp10);

        alphaelem1 = findViewById(R.id.alpha_elem1);
        alphaelem2 = findViewById(R.id.alpha_elem2);
        alphaelem3 = findViewById(R.id.alpha_elem3);
        alphaelem4 = findViewById(R.id.alpha_elem4);

        betaelem1 = findViewById(R.id.beta_elem1);
        betaelem2 = findViewById(R.id.beta_elem2);
        betaelem3 = findViewById(R.id.beta_elem3);
        betaelem4 = findViewById(R.id.beta_elem4);

        thetaelem1 = findViewById(R.id.theta_elem1);
        thetaelem2 = findViewById(R.id.theta_elem2);
        thetaelem3 = findViewById(R.id.theta_elem3);
        thetaelem4 = findViewById(R.id.theta_elem4);

        deltaelem1 = findViewById(R.id.delta_elem1);
        deltaelem2 = findViewById(R.id.delta_elem2);
        deltaelem3 = findViewById(R.id.delta_elem3);
        deltaelem4 = findViewById(R.id.delta_elem4);

        darelem1 = findViewById(R.id.dar_elem1);
        darelem2 = findViewById(R.id.dar_elem2);
        darelem3 = findViewById(R.id.dar_elem3);
        darelem4 = findViewById(R.id.dar_elem4);

        dtrelem1 = findViewById(R.id.dtr_elem1);
        dtrelem2 = findViewById(R.id.dtr_elem2);
        dtrelem3 = findViewById(R.id.dtr_elem3);
        dtrelem4 = findViewById(R.id.dtr_elem4);
        */

        result = findViewById(R.id.result);

        editText = findViewById(R.id.edit_text);
    }

    //-----------------------------------
    //UI updates

    /**
     * The runnable that is used to update the UI at 60Hz.
     *
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (EEGStale) {
                updateUI(EEGText, EEGBuffer);
            }
            if (accelStale) {
                updateUI(accelText, accelBuffer);
            }
            if (alphaStale) {
                updateUI(alphaText, alphaBuffer);
            }
            if (betaStale) {
                updateUI(betaText, betaBuffer);
            }
            if (thetaStale) {
                updateUI(thetaText, thetaBuffer);
            }
            if (deltaStale) {
                updateUI(deltaText, deltaBuffer);
            }
            if (DARStale) {
                updateUI(DARText, DARBuffer);
            }
            if (DTRStale) {
                updateUI(DTRText, DTRBuffer);
            }

            /*
            if (EEGStale) {
                updateEEG();
            }
            if (accelStale) {
                updateAccel();
            }
            if (alphaStale) {
                updateAlpha();
            }
            if (betaStale) {
                updateBeta();
            }
            if (thetaStale) {
                updateTheta();
            }
            if (deltaStale) {
                updateDelta();
            }
            if (DARStale) {
                updateDAR();
            }
            if (DTRStale) {
                updateDTR();
            }
            */

            EEGData data = new EEGData(
                    id,
                    System.currentTimeMillis(),
                    EEG,
                    alpha,
                    beta,
                    theta,
                    delta,
                    DAR,
                    DTR);
            dataSessionList.add(data);

            log(
                    id + "," +
                    System.currentTimeMillis() + "," +
                    EEG + "," +
                    alpha + "," +
                    beta + "," +
                    theta + "," +
                    delta + "," +
                    DAR + "," +
                    DTR + "\n");
            handler.postDelayed(tickUi, 1000);
        }
    };

    /**
     * The following methods update the TextViews in the UI with the data
     * from the buffers.
     */

    private void updateUI(TextView[] textViews, double[] buffer) {
        for (int i = 0; i < textViews.length; i++) {
            textViews[i].setText(df.format(buffer[i]));
        }
    }
    /*
    private void updateAccel() {
        acc_x.setText(df.format(accelBuffer[0]));
        acc_y.setText(df.format(accelBuffer[1]));
        acc_z.setText(df.format(accelBuffer[2]));
    }

    private void updateEEG() {
        tp9.setText(df.format(EEGBuffer[0]));
        fp1.setText(df.format(EEGBuffer[1]));
        fp2.setText(df.format(EEGBuffer[2]));
        tp10.setText(df.format(EEGBuffer[3]));
    }

    private void updateAlpha() {
        alphaelem1.setText(df.format(alphaBuffer[0]));
        alphaelem2.setText(df.format(alphaBuffer[1]));
        alphaelem3.setText(df.format(alphaBuffer[2]));
        alphaelem4.setText(df.format(alphaBuffer[3]));
    }

    private void updateBeta() {
        betaelem1.setText(df.format(betaBuffer[0]));
        betaelem2.setText(df.format(betaBuffer[1]));
        betaelem3.setText(df.format(betaBuffer[2]));
        betaelem4.setText(df.format(betaBuffer[3]));
    }

    private void updateTheta() {
        thetaelem1.setText(df.format( thetaBuffer[0]));
        thetaelem2.setText(df.format( thetaBuffer[1]));
        thetaelem3.setText(df.format( thetaBuffer[2]));
        thetaelem4.setText(df.format( thetaBuffer[3]));
    }

    private void updateDelta() {
        deltaelem1.setText(df.format( deltaBuffer[0]));
        deltaelem2.setText(df.format( deltaBuffer[1]));
        deltaelem3.setText(df.format( deltaBuffer[2]));
        deltaelem4.setText(df.format( deltaBuffer[3]));
    }

    private void updateDAR() {
        darelem1.setText(df.format( DARBuffer[0]));
        darelem2.setText(df.format( DARBuffer[1]));
        darelem3.setText(df.format( DARBuffer[2]));
        darelem4.setText(df.format( DARBuffer[3]));
    }

    private void updateDTR() {
        dtrelem1.setText(df.format( DTRBuffer[0]));
        dtrelem2.setText(df.format( DTRBuffer[1]));
        dtrelem3.setText(df.format( DTRBuffer[2]));
        dtrelem4.setText(df.format(  DTRBuffer[3]));
    }
    */

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.

    /**
     * Checks for changes in available Muse headsets
     */
    static class MuseL extends MuseListener {
        final WeakReference<MainActivity> activityRef;

        MuseL(final WeakReference<MainActivity> activityRef) {
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
        final WeakReference<MainActivity> activityRef;

        ConnectionListener(final WeakReference<MainActivity> activityRef) {
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
        final WeakReference<MainActivity> activityRef;

        DataListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            activityRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }

    /**
     * Logs message to csv file
     * @param string Message to be logged
     */
    private void log(String string) {
        try {
            FileOutputStream fos;
            fos = openFileOutput(FILE_NAME, MODE_APPEND);
            fos.write(string.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "FileNotFoundException", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads data from csv file
     * @return Returns a list of data parsed from file
     */
    public List<EEGData> readData() {
        List<EEGData> data = new ArrayList<>();
        try {
            FileInputStream fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(new DataInputStream(fis));
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null ) {
                String[] tokens = line.split(",");
                EEGData drivingData = new EEGData(
                        Integer.parseInt(tokens[0]),
                        Long.parseLong(tokens[1]),
                        Double.parseDouble(tokens[2]),
                        Double.parseDouble(tokens[3]),
                        Double.parseDouble(tokens[4]),
                        Double.parseDouble(tokens[5]),
                        Double.parseDouble(tokens[6]),
                        Double.parseDouble(tokens[7]),
                        Double.parseDouble(tokens[8]));
                data.add(drivingData);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * Finds average of the various data in the session
     * @param eegDataList Data that is to be processed as a list
     * @param id Session id to filter out data
     * @return Returns processed data
     */
    private EEGData processData(List<EEGData> eegDataList, int id) {
        long time = 0;
        double EEG = 0;
        double alpha = 0;
        double beta = 0;
        double theta = 0;
        double delta = 0;
        double DAR = 0;
        double DTR = 0;

        List<EEGData> finDataList = new ArrayList<>();
        for (EEGData eegData: eegDataList) {
            if (eegData.id == id) {
                finDataList.add(eegData);
            }
        }

        for (EEGData eegData: finDataList) {
            EEG += eegData.EEG;
            alpha += eegData.alpha;
            beta += eegData.beta;
            theta += eegData.theta;
            delta += eegData.delta;
            DAR += eegData.DAR;
            DTR += eegData.DTR;
        }

        int finDataListSize = finDataList.size();
        return new EEGData(
                id,
                time / finDataListSize,
                EEG / finDataListSize,
                alpha / finDataListSize,
                beta / finDataListSize,
                theta / finDataListSize,
                delta / finDataListSize,
                DAR / finDataListSize,
                DTR / finDataListSize
        );
    }

    /**
     * The user has clicked the Classify button.
     * Session id is read and appropriate data is read
     * Output is displayed to user
     */
    public void classify(View view) {
        int id = Integer.parseInt(editText.getText().toString());
        List<EEGData> dataList = readData();
        EEGData resultData = processData(dataList, id);

        if (resultData.DAR > STROKE_THRESHOLD) {
            result.setText(R.string.stroke_positive);
        }
        else {
            result.setText(R.string.stroke_negative);
        }
    }
}
