package com.phatty.androidvideo

import android.graphics.ImageFormat
import android.hardware.Camera
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.phatty.androidvideo.media.H264Encoder
import java.io.IOException

/**
 * TODO Camera.PreviewCallback废弃了, 应该用哪个?
 * @author jinguochong
 * @since  2021/2/23
 */
class PreviewActivity : AppCompatActivity(), SurfaceHolder.Callback, Camera.PreviewCallback {

    private var camera: Camera? = null
    private var encoder: H264Encoder? = null
    private lateinit var surfaceHolder: SurfaceHolder

    private val width = 1280
    private val height = 720
    private val frameRate = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aty_preview)

        val surfaceView: SurfaceView = findViewById(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)

        val go: Button = findViewById(R.id.go_muxer)
        go.setOnClickListener {

        }

        if (supportH264Codec()) {
            Log.e("PreviewActivity", "support H264 hard codec")
        } else {
            Log.e("PreviewActivity", "not support H264 hard codec")
        }
    }

    private fun supportH264Codec(): Boolean {
        // 遍历支持的编码格式信息
        // 从后往前遍历会更快,因为按照audio(size 15) video(size 1)这个顺序排的,前面都是MP3格式
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        for (i in mediaCodecList.codecInfos.size - 1 downTo 0) {
            val codecInfo: MediaCodecInfo = mediaCodecList.codecInfos[i]
            for (type: String in codecInfo.supportedTypes) {
                // Log.e("PreviewActivity", "type: $type")
                if (type.equals("video/avc", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i("PreviewActivity", "enter surfaceCreated method")
        // 目前设定的是，当surface创建后，就打开摄像头开始预览
        camera = Camera.open()
        camera!!.setDisplayOrientation(90)
        val parameters: Camera.Parameters = camera!!.getParameters()
        parameters.previewFormat = ImageFormat.NV21
        parameters.setPreviewSize(width, height)

        try {
            camera?.setParameters(parameters)
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.setPreviewCallback(this)
            camera?.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        encoder = H264Encoder(width, height, frameRate)
        encoder?.startEncoder()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i("PreviewActivity", "enter surfaceChanged method")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i("PreviewActivity", "enter surfaceDestroyed method")

        // 停止预览并释放资源

        // 停止预览并释放资源
        if (camera != null) {
            camera?.setPreviewCallback(null)
            camera?.stopPreview()
            camera?.release()
            camera = null
        }

        encoder?.stopEncoder()
    }


    override fun onPreviewFrame(data: ByteArray, camera: Camera?) {
        Log.w("PreviewActivity", "enter onPreviewFrame")
        encoder?.putData(data)
    }
}