package com.spica.app

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import android.content.ContentValues
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoRecorder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    fun startRecording(onResult: (Boolean) -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    videoCapture
                )

                beginCapture(onResult)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission")
    private fun beginCapture(onResult: (Boolean) -> Unit) {
        val name = "SPICA_VIDEO_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/SPICA")
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        try {
            recording = videoCapture?.output
                ?.prepareRecording(context, outputOptions)
                ?.withAudioEnabled()
                ?.start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Start) {
                        onResult(true)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false)
        }
    }

    fun stopRecording(onStopped: () -> Unit) {
        try {
            recording?.stop()
            recording = null
            onStopped()
        } catch (e: Exception) {
            e.printStackTrace()
            onStopped()
        }
    }
}
