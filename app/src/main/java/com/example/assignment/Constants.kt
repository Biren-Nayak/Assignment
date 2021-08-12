package com.example.assignment

import android.Manifest
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA

const val TAG = "CameraXBasic"
const val REQUEST_CODE_PERMISSIONS = 10
val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
val cameraValues = arrayOf(DEFAULT_FRONT_CAMERA, DEFAULT_BACK_CAMERA)
val cameraImages = arrayOf(R.drawable.ic_outline_camera_front_24, R.drawable.ic_outline_camera_rear_24)