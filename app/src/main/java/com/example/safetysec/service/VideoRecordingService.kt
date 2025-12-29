package com.example.safetysec.service

import android.content.Context
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Video Recording Service
 *
 * Simplified version for alert video upload
 * Note: Actual video recording requires CameraX Video API or MediaRecorder
 */
@Singleton
class VideoRecordingService @Inject constructor(
    private val storage: FirebaseStorage
) {
    companion object {
        private const val TAG = "VideoRecordingService"
        private const val VIDEOS_FOLDER = "alert_videos"
    }

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Upload video to Firebase Storage
     * @param localFilePath Local file path of the video
     * @param alertId Alert ID for organizing in storage
     * @return Download URL of the uploaded video
     */
    suspend fun uploadVideo(
        localFilePath: String,
        alertId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(localFilePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("Video file not found"))
            }

            // Create storage reference
            val videoRef: StorageReference = storage.reference
                .child(VIDEOS_FOLDER)
                .child("$alertId.mp4")

            // Upload file
            val uploadTask = videoRef.putFile(android.net.Uri.fromFile(file))
            uploadTask.await()

            // Get download URL
            val downloadUrl = videoRef.downloadUrl.await()

            // Delete local file after successful upload
            file.delete()

            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading video", e)
            Result.failure(e)
        }
    }

    /**
     * Placeholder for future video recording implementation
     * Video recording in a service is complex and requires:
     * - MediaRecorder API or CameraX Video (not deprecated VideoCapture)
     * - Proper lifecycle management
     * - Background recording permissions (Android 14+)
     */
    fun startRecording(context: Context, alertId: String): Result<String> {
        Log.w(TAG, "Video recording not yet implemented")
        return Result.failure(Exception("Video recording not implemented"))
    }

    /**
     * Release resources
     */
    fun release() {
        // Cleanup if needed
    }
}