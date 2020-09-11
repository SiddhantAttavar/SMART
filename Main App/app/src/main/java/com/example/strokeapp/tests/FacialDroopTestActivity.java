package com.example.strokeapp.tests;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.imageprocessing.CameraFragment;
import com.example.strokeapp.R;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;

import java.io.IOException;
import java.util.Objects;

@SuppressWarnings({"FieldCanBeLocal", "deprecation"})
@SuppressLint("SetTextI18n")
public class FacialDroopTestActivity extends AppCompatActivity {

    //UI elements
    private TextView result;
    private CameraFragment cameraFragment;
    private CameraFragment.ImageAnalyzer imageAnalyzer;

    //Tensorflow related variables
    private Interpreter interpreter;
    private ImageProcessor imageProcessor;
    private TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
    private String FILE_NAME = "facial_droop_model.tflite";

    //The required width and height of the image for the model is 220 * 220
    private final int width = 220;
    private final int height = 220;

    //Object in which we receive our results
    private float[][] results = new float[1][2];

    /**
     * Called when the activity is created
     * Initializes the various camera and image processing related processes
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facial_droop_test);

        //Initialize the Image processing with different processes
        //We need to rotate the image since the camera gives us a rotated image
        //Then we need to resize the image to 220 * 220
        imageProcessor = new ImageProcessor.Builder()
                .add(new Rot90Op())
                .add(new ResizeOp(2 * width, 2 * height, ResizeOp.ResizeMethod.BILINEAR)).build();

        //We initialize the image analyzer to not perform real time analysis
        imageAnalyzer = new CameraFragment.ImageAnalyzer(this, null);

        //Load the interpreter from the /assets/facial_droop_model.tflite file
        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(this, FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Initialize UI
        result = findViewById(R.id.results);
        result.setText("Results: Stroke probability: ");
        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.camera_fragment);

        //We set the image analyzer in the camera fragment
        Objects.requireNonNull(cameraFragment).setup(imageAnalyzer);
    }

    /**
     * Called when the user clicks on the capture image button
     * We get the current image displayed, perform our preprocessing and analysis on it
     * and then display the results to the user
     */
    public void captureImage(View view) {
        //Get the image which is currently being displayed
        Bitmap bitmap = CameraFragment.imageProxytoBitmap(imageAnalyzer.getImage());

        //Process the image using Tensorflow image processor
        tensorImage = TensorImage.fromBitmap(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        //Run the model on the image and get the results in the results array
        interpreter.run(tensorImage.getBuffer(), results);

        //The stroke and no stroke confidence scores are stored in the
        //results[0][0] and results[0][1] variables respectively
        //We display the results to the user
        result.setText(String.format(getString(R.string.results_prompt), results[0][0]));
    }
}