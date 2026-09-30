package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

object AudioRecorderHelper {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null

    fun startRecording(context: Context): File? {
        return try {
            stopRecording() // ensure any previous recorder is released

            val cacheDir = context.cacheDir
            val audioFile = File(cacheDir, "audio_record_${System.currentTimeMillis()}.m4a")
            currentOutputFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            audioFile
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            null
        }
    }

    fun stopRecording(): File? {
        val file = currentOutputFile
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            currentOutputFile = null
        }
        return if (file != null && file.exists() && file.length() > 0) file else null
    }

    fun isRecording(): Boolean = mediaRecorder != null

    fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            currentOutputFile = null
        }
    }
}
