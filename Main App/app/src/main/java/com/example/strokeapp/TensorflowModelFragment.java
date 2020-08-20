package com.example.strokeapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class TensorflowModelFragment extends Fragment implements LifecycleOwner, ImageAnalysis.Analyzer {

    private PreviewView cameraView;

    //Image related variables
    private ImageProxy image;
    private int width, height;

    //Whether to do real time inference or perform at the end
    private boolean runRealTime;

    //Tensorflow related variables
    private Interpreter interpreter;
    public Object results;
    private ImageProcessor imageProcessor;
    private TensorImage tensorImage = new TensorImage(DataType.FLOAT32);

    private final String TAG = "TensorflowModelFragment";
    private final int REQUEST_CODE_PERMISSIONS = 10;
    private String[] REQUIRED_PERMISSIONS = new String[] {Manifest.permission.CAMERA};

    //Camera related variables
    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    private Runnable postAnalysis;

    public TensorflowModelFragment() {
        // Required empty public constructor
    }

    public void setup(String FILE_NAME, boolean runRealTime, int width, int height, Object results, Runnable postanalysis) {
        this.runRealTime = runRealTime;
        this.width = width;
        this.height = height;
        this.results = results;
        this.postAnalysis = postanalysis;
        imageProcessor = new ImageProcessor.Builder().add(new ResizeOp(width, height, ResizeOp.ResizeMethod.BILINEAR)).build();
        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(getActivity(), FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_tensorflow_model, container, false);

        cameraView = root.findViewById(R.id.camera_view);

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(getContext()  , "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(cameraView.createSurfaceProvider());
                    CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                    ImageAnalysis analyzer = new ImageAnalysis.Builder().build();
                    analyzer.setAnalyzer(cameraExecutor, TensorflowModelFragment.this);

                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(TensorflowModelFragment.this, cameraSelector, preview, analyzer);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(getContext(), REQUIRED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED;
    }

    public void doInference() {
        Bitmap bitmap = Bitmap.createScaledBitmap(imageProxytoBitmap(image), width, height, false);
        tensorImage = TensorImage.fromBitmap(bitmap);
        tensorImage = imageProcessor.process(tensorImage);
        interpreter.run(tensorImage.getBuffer(), results);
        postAnalysis.run();
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private Bitmap imageProxytoBitmap(ImageProxy image) {
        Image.Plane[] planes = image.getImage().getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        Log.d(TAG, "Called");
        this.image = image;
        if (runRealTime) {
            doInference();
        }
    }
}