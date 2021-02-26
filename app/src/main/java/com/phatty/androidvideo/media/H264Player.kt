package com.phatty.androidvideo.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.*
import java.nio.ByteBuffer


/**
 * https://juejin.cn/post/6906018591310053389
 * @author jinguochong
 * @since  2021/2/24
 */
class H264Player(private val path: String, surface: Surface) : Runnable {
    private var mediaCodec: MediaCodec? = null

    init {
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            // 视频宽高暂时写死
            val mediaFormat =
                MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 368, 384)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
            mediaCodec!!.configure(mediaFormat, surface, null, 0)
        } catch (e: IOException) {
            // 解码芯片不支持，走软解
            e.printStackTrace()
        }
    }

    override fun run() {
        // 解码 h264
        decodeH264()
    }

    private fun decodeH264() {
        var bytes: ByteArray? = null
        try {
            // 本地 h264 文件路径
            bytes = getBytes(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        var startIndex = 0
        var nextFrameStart: Int
        val totalCount = bytes!!.size
        while (true) {
            if (startIndex >= totalCount) {
                break
            }
            val info = MediaCodec.BufferInfo()
            // 最大应该是 totalCount-1
            nextFrameStart = findFrame(bytes, startIndex + 1, totalCount)
            if (nextFrameStart == -1) {
                break
            }
            val index = mediaCodec!!.dequeueInputBuffer(10 * 1000.toLong())
            Log.e("index", " $index  $nextFrameStart  $startIndex  ")
            // 获取 dsp 成功
            if (index >= 0) {
                // 拿到可用的 ByteBuffer, 这里获取输入流
                val byteBuffer: ByteBuffer = mediaCodec!!.getInputBuffer(index)!!
                byteBuffer.clear()
                // 往 ByteBuffer 中塞入数据, 这里对应显示内容
                byteBuffer.put(bytes, startIndex, nextFrameStart - startIndex)
                // 识别分隔符，找到分隔符对应的索引
                mediaCodec!!.queueInputBuffer(index, 0, nextFrameStart - startIndex, 0, 0)
                startIndex = nextFrameStart
            } else {
                continue
            }


            // 从 ByteBuffer 中获取解码好的数据
            val outIndex = mediaCodec!!.dequeueOutputBuffer(info, 10 * 1000.toLong())
            if (outIndex > 0) {
                try {
                    Thread.sleep(33)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                mediaCodec!!.releaseOutputBuffer(outIndex, true)
            }
        }
    }

    private fun findFrame(bytes: ByteArray, startIndex: Int, totalSize: Int): Int {
        for (i in startIndex until totalSize - 5) {
            if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte()
                && bytes[i + 2] == 0.toByte() && bytes[i + 3] == 1.toByte()
            ) {
                return i
            }
        }
        return -1
    }

    fun play() {
        mediaCodec!!.start()
        Thread(Runnable { this.run() }).start()
    }


    /**
     * 一次性读取文件
     *
     * @param path
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun getBytes(path: String): ByteArray {
        val inputStream: InputStream = DataInputStream(FileInputStream(File(path)))
        var len: Int
        val size = 1024
        var buf: ByteArray
        val bos = ByteArrayOutputStream()
        buf = ByteArray(size)
        while (inputStream.read(buf, 0, size).also { len = it } != -1) {
            bos.write(buf, 0, len)
        }
        buf = bos.toByteArray()
        return buf
    }


}