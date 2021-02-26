package com.phatty.androidvideo.muxer

import android.media.*
import java.io.IOException
import java.nio.ByteBuffer

/**
 * @author jinguochong
 * @since  2021/2/26
 */
class AudioPlayer : Runnable{

    private var mAudioInputBufferSize = 0
    private var mAudioTrack: AudioTrack? = null
    private var mFileDescriptor: String? = null

    @Volatile
    private var mIsPlaying = false

    fun setDataSource(path: String) {
        mFileDescriptor = path
    }

    fun start() {
        Thread(this).start()
    }

    fun stop() {
        mIsPlaying = true
    }

    override fun run() {
        play()
    }

    private fun play() {
        if (mFileDescriptor == null) {
            return
        }
        mIsPlaying = true
        val audioExtractor = MediaExtractor()
        var audioCodec: MediaCodec? = null
        try {
            audioExtractor.setDataSource(mFileDescriptor!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        for (i in 0 until audioExtractor.trackCount) {
            val mediaFormat = audioExtractor.getTrackFormat(i)
            val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mimeType!!.startsWith("audio/")) {
                audioExtractor.selectTrack(i)
                val audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                val maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                val minBufferSize = AudioTrack.getMinBufferSize(
                    audioSampleRate,
                    if (audioChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                mAudioInputBufferSize = if (minBufferSize > 0) minBufferSize * 4 else maxInputSize
                val frameSizeInBytes = audioChannels * 2
                mAudioInputBufferSize =
                    mAudioInputBufferSize / frameSizeInBytes * frameSizeInBytes
                mAudioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    44100,
                    if (audioChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    mAudioInputBufferSize,
                    AudioTrack.MODE_STREAM
                )
                mAudioTrack!!.play()

                //
                try {
                    audioCodec = MediaCodec.createDecoderByType(mimeType!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                audioCodec!!.configure(mediaFormat, null, null, 0)
                break
            }
        }
        if (audioCodec == null) {
            return
        }
        audioCodec.start()
        val decodeBufferInfo = MediaCodec.BufferInfo()
        while (mIsPlaying) {
            val inputIndex = audioCodec.dequeueInputBuffer(10000)
            if (inputIndex < 0) {
                mIsPlaying = false
            }
            val inputBuffer = audioCodec.getInputBuffer(inputIndex)
            inputBuffer!!.clear()
            val sampleSize = audioExtractor.readSampleData(inputBuffer!!, 0)
            if (sampleSize > 0) {
                audioCodec.queueInputBuffer(
                    inputIndex,
                    0,
                    sampleSize,
                    audioExtractor.sampleTime,
                    0
                )
                audioExtractor.advance()
            } else {
                mIsPlaying = false
            }
            var outputIndex = audioCodec.dequeueOutputBuffer(decodeBufferInfo, 10000)
            var outputBuffer: ByteBuffer?
            var chunkPCM: ByteArray
            while (outputIndex >= 0) {
                outputBuffer = audioCodec.getOutputBuffer(outputIndex)
                chunkPCM = ByteArray(decodeBufferInfo.size)
                outputBuffer!![chunkPCM]
                outputBuffer!!.clear()
                mAudioTrack!!.write(chunkPCM, 0, decodeBufferInfo.size)
                audioCodec.releaseOutputBuffer(outputIndex, false)
                outputIndex = audioCodec.dequeueOutputBuffer(decodeBufferInfo, 10000)
            }
        }
        mIsPlaying = false
        audioCodec.stop()
        audioCodec.release()
        audioExtractor.release()

    }
}