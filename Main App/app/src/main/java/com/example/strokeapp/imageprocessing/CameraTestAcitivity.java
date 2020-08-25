package com.example.strokeapp.imageprocessing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.strokeapp.R;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraTestAcitivity extends AppCompatActivity {

    private PreviewView previewView;
    private Preview preview;
    private ImageProcessor imageProcessor;
    private ImageView imageView;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    private ImageAnalyzer imageAnalyzer;

    private int inputWidth, inputHeight;
    private final int cropSize = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test_acitivity);

        previewView = findViewById(R.id.preview_view);
        imageView = findViewById(R.id.display_image_view);

        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .build();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                long lastFpsTimestamp = System.currentTimeMillis();
                imageAnalysis.setAnalyzer(cameraExecutor, imageAnalyzer);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                preview.setSurfaceProvider(previewView.createSurfaceProvider());

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setup(int inputWidth, int inputHeight, ImageAnalyzer imageAnalyzer) {
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        this.imageAnalyzer = imageAnalyzer;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void captureImage(View view) {
    }

    public static class ImageAnalyzer implements ImageAnalysis.Analyzer {
        private ImageProxy image;
        private Bitmap bitmap;
        private Runnable analyzer;
        private int imageRotationDegrees;
        private Context context;
        private YuvToRgbConverter converter;

        public ImageAnalyzer(Context context, Runnable analyzer) {
            this.analyzer = analyzer;
            this.context = context;
            converter = new YuvToRgbConverter(context);
        }

        @Override
        public void analyze(@NonNull ImageProxy image) {
            this.image = image;
            if (analyzer != null) {
                analyzer.run();
            }
        }

        public ImageProxy getImage() {
            return image;
        }

        @SuppressLint("UnsafeExperimentalUsageError")
        public Bitmap getBitmap() {
            if (bitmap == null) {
                imageRotationDegrees = image.getImageInfo().getRotationDegrees();
                bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            }
            converter.yuvToRgb(image.getImage(), bitmap);
            return bitmap;
        }
    }
}