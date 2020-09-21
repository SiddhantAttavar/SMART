package com.example.strokeapp.tests;

import android.Manifest;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"FieldCanBeLocal", "deprecation", "ConstantConditions", "unused", "RedundantSuppression", "DefaultLocale"})
public class ArmWeaknessTestActivity extends AppCompatActivity {

    //UI elements
    private TextView result;
    private ImageView imageView;

    //Posenet takes an image of 257 * 257 as an input
    private final int MODEL_SIZE = 257;

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
    private String FILE_NAME = "posenet_model.tflite";

    //We keep the threshold for stroke as 30 pixels or 12% of the screen size
    private final int STROKE_THRESHOLD = 30;

    //Object in which we receive our results
    Map<Integer, Object> results = new HashMap<>();

    //The indices of the left and right arms in the tflite
    //result are 9 and 10 respectively
    private final int LEFT_ARM = 9;
    private final int RIGHT_ARM = 10;

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
        setContentView(R.layout.activity_arm_weakness_test);

        //Load the interpreter from the /assets/facial_droop_model.tflite file
        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(this, FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Initialize UI
        imageView = findViewById(R.id.image_view);
        result = findViewById(R.id.results);
        result.setText(R.string.test_complete);
    }

    /**
     * Called when the user clicks on the capture image button
     * We get the current image displayed, perform our preprocessing and analysis on it
     * and then display the results to the user
     */
    public void captureImage(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
                //Get the image from the Uri
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                //Process the image using Tensorflow image processor
                tensorImage = TensorImage.fromBitmap(bitmap);
                tensorImage = imageProcessor.process(tensorImage);

                createOutput();

                //Run the model on the image and get the results in the results array
                interpreter.runForMultipleInputsOutputs(new Object[] {tensorImage.getBuffer()}, results);

                float[][][] heatmap = ((float[][][][]) results.get(0))[0];
                float[][][] offsets = ((float[][][][]) results.get(1))[0];

                //Output Width: 22
                //Output Height: 22
                //Output keypoints (Body joints): 17
                int height = heatmap.length;
                int width = heatmap[0].length;
                int keypoints = heatmap[0][0].length;

                //We scan the heatmap for the point in the image
                //containing the highest confidence score for the
                //left and the right hand
                //The actual confidence score is sigmoid(value in heatmap) which is between 0 and 1
                //However since sigmoid is an increasing function we do not have to
                //calculate the actual value to find the maximum confidence score
                int tempLeftX = 0, tempLeftY = 0;
                int tempRightX = 0, tempRightY = 0;
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        if (!Float.isNaN(heatmap[i][j][LEFT_ARM]) && heatmap[i][j][LEFT_ARM] > heatmap[tempLeftY][tempLeftX][LEFT_ARM]) {
                            tempLeftX = j;
                            tempLeftY = i;
                        }

                        if (!Float.isNaN(heatmap[i][j][RIGHT_ARM]) && heatmap[i][j][RIGHT_ARM] > heatmap[tempRightY][tempRightX][RIGHT_ARM]) {
                            tempRightX = j;
                            tempRightY = i;
                        }
                    }
                }

                //We calculate the final position of the left and right hand
                //using the offsets for the joints
                int leftX = (int) (tempLeftX / (float) (width - 1) * tensorImage.getWidth() + offsets[tempLeftY][tempLeftX][LEFT_ARM]);
                int leftY = (int) (tempLeftY / (float) (height - 1) * tensorImage.getHeight() + offsets[tempLeftY][tempLeftX][LEFT_ARM + keypoints]);

                int rightX = (int) (tempRightX / (float) (width - 1) * tensorImage.getWidth() + offsets[tempLeftY][tempLeftX][RIGHT_ARM]);
                int rightY = (int) (tempRightY / (float) (height - 1) * tensorImage.getHeight() + offsets[tempLeftY][tempLeftX][RIGHT_ARM]);

                //If the difference between the Y coordinates of
                //the left and right hands is greater than the STROKE_THRESHOLD
                //then arm weakness is present and the user may have had a stroke
                String stringResult;
                if (Math.abs(rightY - leftY) >= STROKE_THRESHOLD) {
                    stringResult = getString(R.string.stroke_test_result, "Yes");
                }
                else {
                    stringResult = getString(R.string.stroke_test_result, "No");
                }

                result.setText(stringResult);
                ResultsActivity.log(this, ResultsActivity.TESTS, getString(R.string.arm_weakness), stringResult);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates the output map for the posenet model
     * This output is of the form of four float arrays:
     * 1. Heatmap: 1 * 22 * 22 * 17
     * 2. Offsets: 1 * 22 * 22 * 34 (first half is x offset and next half is y offset)
     * 3. Displacement Forward: 1 * 22 * 22 * 32
     * 4. Displacement Backward: 1 * 22 * 22 * 32
     */
    private void createOutput() {
        results = new HashMap<>();

        for (int i = 0; i < interpreter.getOutputTensorCount(); i++) {
            int[] shape = interpreter.getOutputTensor(i).shape();
            results.put(i, new float[shape[0]][shape[1]][shape[2]][shape[3]]);
        }
    }
}