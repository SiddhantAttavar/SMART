package com.example.strokeapp.tests;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.strokeapp.R;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("FieldCanBeLocal")
public class ArmWeaknessTest extends AppCompatActivity {
    //Permissions related constants
    public final int PERMISSION_CODE = 1000;
    public final int IMAGE_CAPTURE_CODE = 1001;

    //Tensorflow related variables
    private final String FILE_NAME = "facial_droop_model.tflite";
    private Interpreter interpreter;

    //Image related variables
    private Uri uri;
    private final int IMAGE_MEAN = 128;
    private final float IMAGE_STD = 128.0f;
    private final int IMG_SIZE = 220;
    private int[] imgValues = new int[IMG_SIZE * IMG_SIZE];
    private float[][] output;

    //UI elements
    private TextView results;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arm_weakness_test);

        interpreter = new Interpreter(getFile());

        //Initialize UI
        results = findViewById(R.id.results);
        imageView = findViewById(R.id.display_image_view);
    }

    //Get file from assets
    private File getFile() {
        try {
            AssetManager am = getAssets();
            InputStream inputStream = am.open(FILE_NAME);
            File file = File.createTempFile("temp", ".tflite");
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return file;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void capture(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)) {
            String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permission, PERMISSION_CODE);
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

    //Convert a Bitmap to a Byte Buffer
    private ByteBuffer imgToBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 3 * IMG_SIZE * IMG_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.rewind();
        bitmap.getPixels(imgValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        bitmap.getPixels(imgValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // loop through all pixels
        int pixel = 0;
        for (int i = 0; i < IMG_SIZE; ++i) {
            for (int j = 0; j < IMG_SIZE; ++j) {
                final int val = imgValues[pixel++];
                // get rgb values from intValues where each int holds the rgb values for a pixel.
                // if quantized, convert each rgb value to a byte, otherwise to a float
                byteBuffer.putFloat((((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat((((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                byteBuffer.putFloat((((val) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }

        return byteBuffer;
    }

    // Resize bitmap to given dimensions
    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Matrix matrix = new Matrix();
        matrix.postScale(((float) newWidth) / bm.getWidth(), ((float) newHeight) / bm.getHeight());
        return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
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
        runPrediction();
    }

    private void runPrediction() {
        Bitmap bitmap_original = null;
        try {
            bitmap_original = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = getResizedBitmap(bitmap_original, IMG_SIZE, IMG_SIZE);
        output = new float[1][2];
        interpreter.run(imgToBuffer(bitmap), output);
        results.setText(getString(R.string.results_prompt, output[0][0]));
    }
}