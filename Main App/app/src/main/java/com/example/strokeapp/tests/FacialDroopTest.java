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

public class FacialDroopTest extends AppCompatActivity {
    //UI elements
    private TextView result;
    private CameraFragment cameraFragment;
    private CameraFragment.ImageAnalyzer imageAnalyzer;

    //Tensorflow related variables
    private Interpreter interpreter;
    private ImageProcessor imageProcessor;
    private TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
    private String FILE_NAME = "facial_droop_model.tflite";

    private final int width = 220;
    private final int height = 220;

    private float[][] results = new float[1][2];

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facial_droop_test);

        imageProcessor = new ImageProcessor.Builder()
                .add(new Rot90Op())
                .add(new ResizeOp(2 * width, 2 * height, ResizeOp.ResizeMethod.BILINEAR)).build();
        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(this, FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Initialize UI
        result = findViewById(R.id.results);
        result.setText("Results: Stroke probability: ");

        imageAnalyzer = new CameraFragment.ImageAnalyzer(this, null);
        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.camera_fragment);
        cameraFragment.setup(imageAnalyzer);
    }

    public void captureImage(View view) {
        Bitmap bitmap = CameraFragment.imageProxytoBitmap(imageAnalyzer.getImage());
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        tensorImage = TensorImage.fromBitmap(bitmap);
        tensorImage = imageProcessor.process(tensorImage);
        interpreter.run(tensorImage.getBuffer(), results);
        result.setText(String.format(getString(R.string.results_prompt), results[0][0]));
    }
}