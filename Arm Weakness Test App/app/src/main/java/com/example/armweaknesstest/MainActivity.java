package com.example.armweaknesstest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import static java.lang.Math.abs;
import static java.lang.Math.exp;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;
    Button button;
    TextView textView;

    Person person = new Person();

    Uri uri;

    public final int PERMISSION_CODE = 1000;
    public final int IMAGE_CAPTURE_CODE = 1001;

    // presets for rgb conversion
    private static final int RESULTS_TO_SHOW = 2;

    // options for model interpreter
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    // tflite graph
    private Interpreter tflite;
    // holds all the possible labels for model
    private List<String> labelList = new ArrayList<String>();
    // holds the selected image data as bytes
    private ByteBuffer imgData = null;
    // holds the probabilities of each label for non-quantized graphs
    private ArrayList<Float[][][][]> labelProbArray = null;
    // array that holds the labels with the highest probabilities
    private String[] topLables = null;
    // array that holds the highest probabilities
    private String[] topConfidence = null;

    HashMap<Integer, Object> hashMap = new HashMap<>();

    private float[][][][] heatmaps;
    private float[][][][] offsets;

    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;

    // input image dimensions for the Inception Model
    private int IMG_SHAPE = 257;
    private int DIM_PIXEL_SIZE = 3;

    // int array to hold image data
    private int[] intValues;

    // priority queue that will hold the top results from the CNN
    private PriorityQueue<Map.Entry<String, Float>> sortedLabels =
            new PriorityQueue<>(
                    RESULTS_TO_SHOW,
                    new Comparator<Map.Entry<String, Float>>() {
                        @Override
                        public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
                            return (o1.getValue()).compareTo(o2.getValue());
                        }
                    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        button = findViewById(R.id.button);
        textView = findViewById(R.id.text_view);

        // initialize array that holds image data
        intValues = new int[IMG_SHAPE * IMG_SHAPE];

        try {
            tflite = new Interpreter(loadModelFile());
        }
        catch (Exception e){
            e.printStackTrace();
        }

        imgData = ByteBuffer.allocateDirect(4 * IMG_SHAPE * IMG_SHAPE * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());

        labelList.add("No Stroke");
        labelList.add("Stroke");

        // initialize array to hold top labels
        topLables = new String[RESULTS_TO_SHOW];
        // initialize array to hold top probabilities
        topConfidence = new String[RESULTS_TO_SHOW];
    }

    // converts bitmap to byte array which is passed in the tflite graph
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < IMG_SHAPE; ++i) {
            for (int j = 0; j < IMG_SHAPE; ++j) {
                final int val = intValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
    }

    // resizes bitmap to given dimensions
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor assetFileDescriptor = this.getAssets().openFd("model.tflite");
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void onClick(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, PERMISSION_CODE);
            }
            else {
                openCamera();
            }
        }
        else {
            openCamera();
        }
    }

    private void openCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "New Picture");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "From the camera");

        uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case (PERMISSION_CODE): {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }
                else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            imageView.setImageURI(uri);
        }
        Log.d("MainActivity:", "Called onActivityResult()");
        doInference();
    }

    @SuppressLint("ShowToast")
    public void doInference() {
        Bitmap bitmap_original = null;
        Log.d("MainActivity: ", "Called doInference()");
        try {
            bitmap_original = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Object[] objects = new Object[1];
        objects[0] = imgData;

        Bitmap bitmap = getResizedBitmap(bitmap_original, IMG_SHAPE, IMG_SHAPE);
        convertBitmapToByteBuffer(bitmap);

        hashMap = initOuputMap();

        tflite.runForMultipleInputsOutputs(objects, hashMap);

        if (Float.isNaN(((float[][][][]) Objects.requireNonNull(hashMap.get(0)))[0][0][0][0])) {
            Toast.makeText(this, "NaN", Toast.LENGTH_LONG).show();
        }

        Log.d("MainActivity", String.valueOf(Float.isNaN(((float[][][][]) hashMap.get(0))[0][0][0][0]))) ;
        Log.d("MainActivity", String.valueOf(((float[][][][]) hashMap.get(0))[0][0][0][0]));

        getValues();
        getResult();
        Toast.makeText(this, "Complete", Toast.LENGTH_LONG).show();
    }

    private HashMap<Integer, Object> initOuputMap() {
        HashMap<Integer, Object> outputMap = new HashMap<>();

        int[] heatmapsShape = tflite.getOutputTensor(0).shape();
        Object heatmaps = new float[heatmapsShape[0]][heatmapsShape[1]][heatmapsShape[2]][heatmapsShape[3]];
        outputMap.put(0, heatmaps);

        int[] offsetsShape = tflite.getOutputTensor(1).shape();
        Object offsets = new float[offsetsShape[0]][offsetsShape[1]][offsetsShape[2]][offsetsShape[3]];
        outputMap.put(1, offsets);

        int[] dfwdShape = tflite.getOutputTensor(2).shape();
        final Object dfwd = new float[dfwdShape[0]][dfwdShape[1]][dfwdShape[2]][dfwdShape[3]];
        outputMap.put(2, dfwd);

        int[] dbwdShape = tflite.getOutputTensor(3).shape();
        final Object dbwd = new float[dbwdShape[0]][dbwdShape[1]][dbwdShape[2]][dbwdShape[3]];
        outputMap.put(3, dbwd);

        return outputMap;
    }

    private void getResult() {
        int left_wrist = person.keyPoints[9].position.y;
        int right_wrist = person.keyPoints[10].position.y;

        Log.d("MainActivity ", String.valueOf(left_wrist));
        Log.d("MainActivity ", String.valueOf(left_wrist));

        if(left_wrist != right_wrist) {
            textView.setText(labelList.get(1));
        }
        else {
            textView.setText(labelList.get(0));
        }
    }

    private void getValues() {
        heatmaps = (float[][][][]) hashMap.get(0);
        offsets = (float[][][][]) hashMap.get(1);

        int height = heatmaps[0].length;
        int width = heatmaps[0][0].length;
        int numKeyPoints = heatmaps[0][0][0].length;

        int[][] keyPointPositions = new int[numKeyPoints][2];

        for (int keypoint = 0; keypoint < numKeyPoints; keypoint++) {
            float maxVal = heatmaps[0][0][0][keypoint];
            int maxRow = 0;
            int maxCol = 0;

            for (int row = 0; row < height; row++) {
                for (int col = 0; col > width; col++) {
                    if (heatmaps[0][row][col][keypoint] > maxVal) {
                        maxVal = heatmaps[0][row][col][keypoint];
                        maxRow = row;
                        maxCol = col;
                    }
                }
            }

            keyPointPositions[keypoint] = new int[2];
            keyPointPositions[keypoint][0] = maxRow;
            keyPointPositions[keypoint][1] = maxCol;
        }

        int[] xCoords = new int[numKeyPoints];
        int[] yCoords = new int[numKeyPoints];
        float[] confidenceScores = new float[numKeyPoints];

        for (int i = 0; i < numKeyPoints; i++) {
            int[] p = keyPointPositions[i];
            int positionY = p[0];
            int positionX = p[0];

            yCoords[i] = (int) (p[0] / ( (float) height - 1) * IMG_SHAPE + offsets[0][positionY][positionX][i]);
            xCoords[i] = (int) (p[0] / ( (float) width - 1) * IMG_SHAPE + offsets[0][positionY][positionX][i + numKeyPoints]);
            confidenceScores[i] = sigmoid(heatmaps[0][positionY][positionX][i]);
        }

        KeyPoint[] keyPointsList = new KeyPoint[numKeyPoints];

        float totalscore = 0.0f;

        for (int i = 0; i < numKeyPoints; i++) {
            keyPointsList[i] = new KeyPoint();
            BodyPart item = BodyPart.values()[i];
            keyPointsList[i].bodyPart = item;
            keyPointsList[i].position.x = xCoords[i];
            keyPointsList[i].position.y = yCoords[i];
            totalscore += confidenceScores[i];
        }

        person.keyPoints = keyPointsList;
        person.score = totalscore / numKeyPoints;

        if (((float[][][][]) hashMap.get(0))[0][0][0][0] == Float.NaN) {
            Toast.makeText(this, "NaN", Toast.LENGTH_LONG);
        }
    }

    private float sigmoid(float x) {
        return (float) (1.0 / (1.0 + exp(-x)));
    }

    enum BodyPart {
        NOSE,
        LEFT_EYE,
        RIGHT_EYE,
        LEFT_EAR,
        RIGHT_EAR,
        LEFT_SHOULDER,
        RIGHT_SHOULDER,
        LEFT_ELBOW,
        RIGHT_ELBOW,
        LEFT_WRIST,
        RIGHT_WRIST,
        LEFT_HIP,
        RIGHT_HIP,
        LEFT_KNEE,
        RIGHT_KNEE,
        LEFT_ANKLE,
        RIGHT_ANKLE
    }

    class Position {
        int x = 0;
        int y = 0;
    }

    class Person {
        KeyPoint[] keyPoints;
        float score = 0.0f;
    }

    class KeyPoint {
        BodyPart bodyPart = BodyPart.NOSE;
        Position position = new Position();
        float score = 0.0f;
    }
}

