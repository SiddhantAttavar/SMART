package com.example.strokeapp.tests;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.strokeapp.imageprocessing.CameraFragment;
import com.example.strokeapp.R;

import java.util.List;

import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.ImageOrientation;
import ai.fritz.vision.ModelVariant;
import ai.fritz.vision.poseestimation.FritzVisionPosePredictor;
import ai.fritz.vision.poseestimation.FritzVisionPoseResult;
import ai.fritz.vision.poseestimation.Keypoint;
import ai.fritz.vision.poseestimation.Pose;
import ai.fritz.vision.poseestimation.PoseOnDeviceModel;

public class ArmWeaknessTest extends AppCompatActivity {
    //UI elements
    private TextView result;
    private CameraFragment cameraFragment;
    private CameraFragment.ImageAnalyzer imageAnalyzer;

    private FritzVisionPosePredictor predictor;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arm_weakness_test);

        PoseOnDeviceModel onDeviceModel = FritzVisionModels.getHumanPoseEstimationOnDeviceModel(ModelVariant.ACCURATE);
        predictor = FritzVision.PoseEstimation.getPredictor(onDeviceModel);

        //Initialize UI
        result = findViewById(R.id.results);
        result.setText("Results: Stroke probability: ");

        imageAnalyzer = new CameraFragment.ImageAnalyzer(this, null);
        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.camera_fragment);
        cameraFragment.setup(imageAnalyzer);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void captureImage(View view) throws CameraAccessException {
        Bitmap bitmap = imageAnalyzer.getBitmap();

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        ImageOrientation imageOrientation = FritzVisionOrientation.getImageOrientationFromCamera(this, manager.getCameraIdList()[0]);
        FritzVisionImage fritzVisionImage = FritzVisionImage.fromBitmap(bitmap, imageOrientation);

        FritzVisionPoseResult poseResult = predictor.predict(fritzVisionImage);
        List<Pose> poses = poseResult.getPoses();

        if (poses.size() > 0) {
            Pose pose = poses.get(0);
            Keypoint[] keypoints = pose.getKeypoints();
            Toast.makeText(this, String.valueOf(Math.abs(keypoints[9].getPosition().y - keypoints[10].getPosition().y)), Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, "Please take another image", Toast.LENGTH_LONG).show();
        }
    }
}