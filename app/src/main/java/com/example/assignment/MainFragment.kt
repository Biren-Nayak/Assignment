package com.example.assignment

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.assignment.databinding.FragmentMainBinding
import dev.bmcreations.scrcast.ScrCast
import java.util.concurrent.ExecutorService


class MainFragment: Fragment() {

    private lateinit var binding: FragmentMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var index = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val recorder = ScrCast.use(requireActivity())
        configure(recorder)
        var isCapturing = false

        binding = FragmentMainBinding.inflate(inflater, container, false)
        // Request camera permissions
        binding.cameraFlipPosition.setImageDrawable(ContextCompat.getDrawable(requireContext(), cameraImages[index]))
        permissionPrompt()


        val side = (resources.configuration.screenHeightDp / 1.75).toInt()

        val params: ViewGroup.LayoutParams = binding.previewContainer.layoutParams
        params.width = side
        params.height = side
        binding.previewContainer.layoutParams = params

        binding.button.setOnClickListener {
            index = (index + 1)%2
            binding.cameraFlipPosition.setImageDrawable(ContextCompat.getDrawable(requireContext(), cameraImages[index]))
            permissionPrompt()
        }

        binding.imageButton.setOnClickListener{
            findNavController().navigate(R.id.action_mainFragment_to_imagePickerFragment)
        }

        binding.imagePicker.setOnClickListener {
            if(binding.background.isEnabled){
                binding.background.setImageDrawable(null)
                binding.background.isEnabled = false
                binding.imagePicker.setImageDrawable(ContextCompat.getDrawable(requireContext() ,R.drawable.ic_outline_add_photo_alternate_24))
            }
            else{
                getContent.launch("image/*")
                binding.background.isEnabled = true
                binding.imagePicker.setImageDrawable(ContextCompat.getDrawable(requireContext() ,R.drawable.ic_outline_remove_circle_outline_24))
            }
        }

        binding.capture.setOnClickListener {
            val imgRes: Drawable?
            if(isCapturing){
                imgRes = ContextCompat.getDrawable(requireContext(),R.drawable.ic_baseline_camera_24)
                binding.capture.setImageDrawable(imgRes)
                recorder.stopRecording()
            }else{
                imgRes = ContextCompat.getDrawable(requireContext(),R.drawable.ic_baseline_circle_24 )
                binding.capture.setImageDrawable(imgRes)
                recorder.record()
            }
            isCapturing = !isCapturing

        }

        var dx:Float? = 0.0F
        var dy:Float? = 0.0F

        binding.previewContainer.setOnTouchListener { v, event ->
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = v?.x?.minus(event.rawX)
                    dy = v?.y?.minus(event.rawY)
                }
                MotionEvent.ACTION_MOVE -> {
                    v?.animate()
                        ?.x(event.rawX + dx!!)
                        ?.y(event.rawY + dy!!)
                        ?.setDuration(0)
                        ?.start()
                }
            }
            true

        }


        return binding.root
    }

    private fun configure(recorder: ScrCast) {
        recorder.apply {
            // configure options via DSL
            options {
                video {
                    maxLengthSecs = 360
                }
                storage {
                    directoryName = "Cast-sample"
                }
                notification {
                    title = "Recording Started"
                    description = "recording session in progress"
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_camcorder)!!.toBitmap()
                    channel {
                        id = "1337"
                        name = "Recording Service"
                    }
                    showStop = true
                    showPause = true
                    showTimer = true
                }
                moveTaskToBack = false
                startDelayMs = 500
            }
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        binding.background.setImageURI(uri)
    }

    private fun permissionPrompt() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permissions not granted by the user.", LENGTH_SHORT).show()
                requireActivity().finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector =
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraValues[index], preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}