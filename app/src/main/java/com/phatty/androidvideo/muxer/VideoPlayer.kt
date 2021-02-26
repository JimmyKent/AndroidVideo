package com.phatty.androidvideo.muxer

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import java.io.IOException

/**
 * @author jinguochong
 * @since  2021/2/26
 */
class VideoPlayer : Runnable {
    private var mAssetFileDescriptor: String? = null

    @Volatile
    private var mIsPlaying = false
    private var mSurface: Surface? = null

    @Volatile
    private var mEOF = false

    fun setDataSource(path: String) {
        mAssetFileDescriptor = path
    }

    fun start() {
        Thread(this).start()
    }

    fun stop() {
        mIsPlaying = true
    }

    fun setSurface(surface: Surface) {
        mSurface = surface
    }

    override fun run() {
        play()
    }

    private fun play() {
        val videoExtractor = MediaExtractor()
        var videoCodec: MediaCodec? = null
        var startWhen: Long = 0
        try {
            videoExtractor.setDataSource(mAssetFileDescriptor!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var firstFrame = false
        for (i in 0 until videoExtractor.trackCount) {
            val mediaFormat = videoExtractor.getTrackFormat(i)
            val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mimeType!!.startsWith("video/")) {
                videoExtractor.selectTrack(i)
                try {
                    videoCodec = MediaCodec.createDecoderByType(mimeType!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                videoCodec!!.configure(mediaFormat, mSurface, null, 0)
                break
            }
        }
        if (videoCodec == null) {
            return
        }
        videoCodec.start()
        while (!mEOF) {
            val bufferInfo = MediaCodec.BufferInfo()
            val inputBuffers = videoCodec.inputBuffers
            val inputIndex = videoCodec.dequeueInputBuffer(10000)
            if (inputIndex > 0) {
                val byteBuffer = inputBuffers[inputIndex]
                val sampleSize = videoExtractor.readSampleData(byteBuffer, 0)
                if (sampleSize > 0) {
                    // 可以通过ffprobe获取每帧信息 https://www.jianshu.com/p/ba5afa513798
                    // ffprobe -show_frames 1587818594588915.mp4 > test.log
                    Log.i("sampleTime", "video: ${videoExtractor.sampleTime}")
                    videoCodec.queueInputBuffer(
                        inputIndex,
                        0,
                        sampleSize,
                        videoExtractor.sampleTime,
                        0
                    )
                    videoExtractor.advance()
                } else {
                    videoCodec.queueInputBuffer(
                        inputIndex,
                        0,
                        0,
                        0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                }
            }
            when (val outputIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10000)) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> videoCodec.outputBuffers
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                else -> {
                    if (!firstFrame) {
                        startWhen = System.currentTimeMillis()
                        firstFrame = true
                    }
                    val sleepTime =
                        bufferInfo.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen)
                    if (sleepTime > 0) {
                        SystemClock.sleep(sleepTime)
                    }
                    videoCodec.releaseOutputBuffer(outputIndex, true)
                }
            }
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                mEOF = true
                break
            }
        }
        mEOF = false
        videoCodec.stop()
        videoCodec.release()
        videoExtractor.release()
    }
}