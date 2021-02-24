package com.phatty.androidvideo.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Environment
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

/**
 * 最后生成的 .h264 文件不能直接在播放器播放，但是可以通过 ffplay 播放
 * ffplay test.h264
 * https://www.jianshu.com/p/a2c291c1df7f Camera 视频采集，H264 编码保存
 * @author jinguochong
 * @since  2021/2/23
 */
class H264Encoder(width: Int, height: Int, frameRate: Int) {
    private var mediaCodec: MediaCodec? = null
    var isRuning = false
    private val width: Int
    private val height: Int
    private val framerate: Int
    lateinit var configbyte: ByteArray
    private var outputStream: BufferedOutputStream? = null
    var yuv420Queue = ArrayBlockingQueue<ByteArray>(10)

    init {
        this.width = width
        this.height = height
        this.framerate = frameRate
        val mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height)
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
            mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec!!.start()
            createfile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createfile() {
        val path = Environment.getExternalStorageDirectory().absolutePath + "/test.h264"
        Log.e("jgcc", path)
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
        try {
            outputStream = BufferedOutputStream(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun putData(buffer: ByteArray) {
        if (yuv420Queue.size >= 10) {
            yuv420Queue.poll()
        }
        yuv420Queue.add(buffer)
    }

    /***
     * 开始编码
     */
    fun startEncoder() {
        Thread(Runnable {
            isRuning = true
            var input: ByteArray? = null
            var pts: Long = 0
            var generateIndex: Long = 0
            while (isRuning) {
                Log.i("jgccc", "yuv420Queue.size: ${yuv420Queue.size}")
                if (yuv420Queue.size > 0) {
                    input = yuv420Queue.poll()
                    val yuv420sp = ByteArray(width * height * 3 / 2)
                    // 必须要转格式，否则录制的内容播放出来为绿屏
                    NV21ToNV12(input, yuv420sp, width, height)
                    input = yuv420sp
                }
                if (input != null) {
                    try {
                        val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(-1)
                        if (inputBufferIndex >= 0) {
                            pts = computePresentationTime(generateIndex)
                            Log.i("jgccc", "pts: $pts")
                            val inputBuffer: ByteBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)!!
                            inputBuffer.clear()
                            inputBuffer.put(input)
                            mediaCodec!!.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                input.size,
                                System.currentTimeMillis(),
                                0
                            )
                            generateIndex += 1
                        }
                        val bufferInfo = MediaCodec.BufferInfo()
                        var outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(
                            bufferInfo,
                            TIMEOUT_USEC.toLong()
                        )
                        while (outputBufferIndex >= 0) {
                            val outputBuffer: ByteBuffer = mediaCodec!!.getOutputBuffer(outputBufferIndex)!!
                            val outData = ByteArray(bufferInfo.size)
                            outputBuffer[outData]
                            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                configbyte = ByteArray(bufferInfo.size)
                                configbyte = outData
                            } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_SYNC_FRAME) {
                                val keyframe =
                                    ByteArray(bufferInfo.size + configbyte.size)
                                System.arraycopy(
                                    configbyte,
                                    0,
                                    keyframe,
                                    0,
                                    configbyte.size
                                )
                                System.arraycopy(
                                    outData,
                                    0,
                                    keyframe,
                                    configbyte.size,
                                    outData.size
                                )
                                outputStream!!.write(keyframe, 0, keyframe.size)
                            } else {
                                outputStream!!.write(outData, 0, outData.size)
                            }
                            mediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
                            outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(
                                bufferInfo,
                                TIMEOUT_USEC.toLong()
                            )
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                } else {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }

            // 停止编解码器并释放资源
            try {
                Log.i("jgcc", "mediaCodec!!.stop")
                mediaCodec!!.stop()
                mediaCodec!!.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 关闭数据流
            try {
                outputStream!!.flush()
                outputStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
    }

    /**
     * 停止编码数据
     */
    fun stopEncoder() {
        isRuning = false
    }

    private fun NV21ToNV12(nv21: ByteArray?, nv12: ByteArray?, width: Int, height: Int) {
        if (nv21 == null || nv12 == null) return
        val framesize = width * height
        var i = 0
        var j = 0
        System.arraycopy(nv21, 0, nv12, 0, framesize)
        i = 0
        while (i < framesize) {
            nv12[i] = nv21[i]
            i++
        }
        j = 0
        while (j < framesize / 2) {
            nv12[framesize + j - 1] = nv21[j + framesize]
            j += 2
        }
        j = 0
        while (j < framesize / 2) {
            nv12[framesize + j] = nv21[j + framesize - 1]
            j += 2
        }
    }

    /**
     * 根据帧数生成时间戳
     */
    private fun computePresentationTime(frameIndex: Long): Long {

        return 132 + frameIndex * 1000000 / framerate
    }

    companion object {
        private const val TIMEOUT_USEC = 12000
    }


}
