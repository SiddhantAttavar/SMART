package com.example.strokeapp.tests;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.strokeapp.R;
import com.example.strokeapp.results.ResultsActivity;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;

import java.io.IOException;

@SuppressWarnings({"FieldCanBeLocal", "deprecation"})
@SuppressLint("SetTextI18n")
public class FacialDroopTestActivity extends AppCompatActivity {

    //UI elements
    private TextView result;
    private ImageView imageView;

    //The required width and height of the image for the model is 220 * 220
    private final int MODEL_SIZE = 220;

    //Initialize the Image processing with different processes
    //First we have to resize this model and then
    //normalize it so that all pixel values are in the range [-1, 1]
    private ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new ResizeOp(MODEL_SIZE, MODEL_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(new NormalizeOp(128.0f, 128.0f))
            .build();
    private TensorImage tensorImage = new TensorImage(DataType.FLOAT32);

    //Tensorflow related variables
    private Interpreter interpreter;
    private String FILE_NAME = "facial_droop_model.tflite";

    //Object in which we receive our results
    private float[][] results = new float[1][2];

    //Permission codes
    public final int PERMISSION_CODE = 1000;
    public final int IMAGE_CAPTURE_CODE = 1001;

    //Uri for receiving the image from the camera
    private Uri uri;

    /**
     * Called when the activity is created
     * Initializes the various camera and image processing related processes
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facial_droop_test);

        //Load the interpreter from the /assets/facial_droop_model.tflite file
        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(this, FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Initialize UI
        imageView = findViewById(R.id.image_view);
        result = findViewById(R.id.results);
        result.setText("Results: Stroke probability: ");
    }

    /**
     * Called when the user clicks on the capture image button
     * We get the current image displayed, perform our preprocessing and analysis on it
     * and then display the results to the user
     */
    public void captureImage(View view) {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            openCamera();
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
        }
        else {
            openCamera();
        }
    }

    /**
     * Called when the user answers to a permission request
     * If the permissions are granted we carry on with the process
     * @param requestCode Request Code related to the permission
     * @param permissions Permissions which have been granted/denied
     * @param grantResults The responses of the user for each permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
        else {
            Toast.makeText(this, "This test cannot be conducted without camera access", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts an Intent to the device camera to capture the image
     */
    private void openCamera() {
        uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    /**
     * Called after the user has captured the image
     * We the get the image and perform our processing
     * @param requestCode Code of the action that was performed
     * @param resultCode Result of the action Successful/Not Successful
     * @param data Intent carrying the data from the action
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            imageView.setImageURI(uri);

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                //Process the image using Tensorflow image processor
                tensorImage = TensorImage.fromBitmap(bitmap);
                tensorImage = imageProcessor.process(tensorImage);

                //Run the model on the image and get the results in the results array
                interpreter.run(tensorImage.getBuffer(), results);

                //The stroke and no stroke confidence scores are stored in the
                //results[0][0] and results[0][1] variables respectively
                //If the confidence score of stroke is greater than that of no stroke
                //the user may have had a stroke
                //We display the results to the user and log this result
                String stringResult;
                if (results[0][0] > results[0][1]) {
                    stringResult = getString(R.string.stroke_test_result, "Yes");
                }
                else {
                    stringResult = getString(R.string.stroke_test_result, "No");
                }

                result.setText(stringResult);
                ResultsActivity.log(this, ResultsActivity.TESTS, getString(R.string.facial_droop), stringResult);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}