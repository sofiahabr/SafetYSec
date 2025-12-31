package com.example.safetysec.service

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Video Recording Service - CameraX Implementation
 *
 * Handles 30-second video recording for alerts with automatic upload to Firebase Storage
 * Uses CameraX Video API for reliable background recording
 */
@Singleton
class VideoRecordingService @Inject constructor(
    private val storage: FirebaseStorage
) {
    companion object {
        private const val TAG = "VideoRecordingService"
        private const val VIDEOS_FOLDER = "alert_videos"
        private const val RECORDING_DURATION_MS = 30_000L // 30 seconds
        private const val MAX_UPLOAD_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
    }

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var recordingJob: Job? = null

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if audio permission is granted
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Initialize camera for video recording
     * @param context Application context
     * @param lifecycleOwner Lifecycle owner (typically the service)
     */
    suspend fun initializeCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner
    ): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (!hasCameraPermission(context)) {
                return@withContext Result.failure(
                    SecurityException("Camera permission not granted")
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()

            // Create recorder
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(
                        Quality.HD,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                )
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            // Bind to lifecycle - Use FRONT camera for user-facing recording
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    videoCapture
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            Result.failure(e)
        }
    }

    /**
     * Start recording video for an alert
     * @param context Application context
     * @param alertId Alert ID for file naming
     * @param durationSeconds Recording duration (default 30 seconds)
     * @return Result with video file
     */
    suspend fun startRecording(
        context: Context,
        alertId: String,
        durationSeconds: Int = 30
    ): Result<File> = withContext(Dispatchers.Main) {
        try {
            if (videoCapture == null) {
                return@withContext Result.failure(
                    IllegalStateException("Camera not initialized. Call initializeCamera() first.")
                )
            }

            // Create output file
            val outputFile = File(
                context.getExternalFilesDir(null),
                "alert_${alertId}_${System.currentTimeMillis()}.mp4"
            )

            val outputOptions = FileOutputOptions.Builder(outputFile).build()

            val recorder = videoCapture!!.output
            val executor: Executor = ContextCompat.getMainExecutor(context)

            // Use CompletableDeferred for async result
            val recordingComplete = CompletableDeferred<File>()

            // Start recording
            recording = recorder.prepareRecording(context, outputOptions)
                .apply {
                    // Only add audio if permission is granted
                    if (hasAudioPermission(context)) {
                        withAudioEnabled()
                    }
                }
                .start(executor) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "Recording started for alert: $alertId")
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (recordEvent.hasError()) {
                                Log.e(
                                    TAG,
                                    "Recording error: ${recordEvent.error}",
                                    recordEvent.cause
                                )
                                recordingComplete.completeExceptionally(
                                    Exception("Recording failed: ${recordEvent.error}")
                                )
                            } else {
                                Log.d(TAG, "Recording completed: ${outputFile.absolutePath}")
                                recordingComplete.complete(outputFile)
                            }
                            recording = null
                        }
                        is VideoRecordEvent.Status -> {
                            Log.d(TAG, "Recording status: ${recordEvent.recordingStats.numBytesRecorded} bytes")
                        }
                    }
                }

            // Auto-stop after duration
            recordingJob = CoroutineScope(Dispatchers.Main).launch {
                delay(durationSeconds * 1000L)
                stopRecording()
            }

            // Wait for recording to complete
            val file = recordingComplete.await()
            Result.success(file)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            Result.failure(e)
        }
    }

    /**
     * Stop recording manually
     */
    fun stopRecording() {
        recording?.stop()
        recordingJob?.cancel()
        recording = null
        recordingJob = null
    }

    /**
     * Record and upload video for alert (all-in-one function)
     * @param context Application context
     * @param alertId Alert ID
     * @return Result with Firebase Storage download URL
     */
    suspend fun recordAndUploadVideo(
        context: Context,
        alertId: String
    ): Result<String> {
        return try {
            // Start recording
            val recordingResult = startRecording(context, alertId)

            when {
                recordingResult.isSuccess -> {
                    val videoFile = recordingResult.getOrThrow()
                    // Upload with retry logic
                    uploadVideoWithRetry(videoFile, alertId)
                }
                else -> {
                    val error = recordingResult.exceptionOrNull()
                    Log.e(TAG, "Recording failed", error)
                    Result.failure(error ?: Exception("Recording failed"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Record and upload failed", e)
            Result.failure(e)
        }
    }

    /**
     * Upload video to Firebase Storage with retry logic
     * @param localFile Local video file
     * @param alertId Alert ID for organizing in storage
     * @param maxRetries Maximum number of retry attempts
     * @return Download URL of the uploaded video
     */
    private suspend fun uploadVideoWithRetry(
        localFile: File,
        alertId: String,
        maxRetries: Int = MAX_UPLOAD_RETRIES
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!localFile.exists()) {
            return@withContext Result.failure(Exception("Video file not found: ${localFile.absolutePath}"))
        }

        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "Upload attempt ${attempt + 1} of $maxRetries")

                // Create storage reference
                val fileName = "${alertId}_${System.currentTimeMillis()}.mp4"
                val videoRef: StorageReference = storage.reference
                    .child(VIDEOS_FOLDER)
                    .child(fileName)

                // Upload file
                val uploadTask = videoRef.putFile(android.net.Uri.fromFile(localFile))
                uploadTask.await()

                // Get download URL
                val downloadUrl = videoRef.downloadUrl.await()

                // Delete local file after successful upload
                try {
                    localFile.delete()
                    Log.d(TAG, "Local video file deleted after upload")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete local file", e)
                }

                return@withContext Result.success(downloadUrl.toString())

            } catch (e: Exception) {
                Log.e(TAG, "Upload attempt ${attempt + 1} failed", e)

                if (attempt < maxRetries - 1) {
                    // Wait before retrying
                    delay(RETRY_DELAY_MS)
                } else {
                    // Last attempt failed
                    return@withContext Result.failure(
                        Exception("Upload failed after $maxRetries attempts: ${e.message}")
                    )
                }
            }
        }

        Result.failure(Exception("Upload failed unexpectedly"))
    }

    /**
     * Upload existing video file to Firebase Storage
     * @param localFilePath Local file path of the video
     * @param alertId Alert ID for organizing in storage
     * @return Download URL of the uploaded video
     */
    suspend fun uploadVideo(
        localFilePath: String,
        alertId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val file = File(localFilePath)
        uploadVideoWithRetry(file, alertId)
    }

    /**
     * Delete video from Firebase Storage
     * @param videoUrl Full download URL of the video
     */
    suspend fun deleteVideo(videoUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val videoRef = storage.getReferenceFromUrl(videoUrl)
            videoRef.delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete video", e)
            Result.failure(e)
        }
    }

    /**
     * Get video file size in MB
     */
    fun getVideoFileSize(file: File): Double {
        return if (file.exists()) {
            file.length() / (1024.0 * 1024.0) // Convert to MB
        } else {
            0.0
        }
    }

    /**
     * Clean up old video files from local storage
     * @param context Application context
     * @param daysOld Files older than this many days will be deleted
     */
    suspend fun cleanupOldVideos(
        context: Context,
        daysOld: Int = 7
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val videoDir = context.getExternalFilesDir(null) ?: return@withContext Result.success(0)
            val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)

            var deletedCount = 0
            videoDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("alert_") && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted old video: ${file.name}")
                    }
                }
            }

            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old videos", e)
            Result.failure(e)
        }
    }

    /**
     * Release camera resources
     */
    fun release() {
        stopRecording()
        videoCapture = null
        Log.d(TAG, "Video recording service released")
    }
}