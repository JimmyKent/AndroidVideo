package com.phatty.androidvideo

import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.phatty.androidvideo.media.H264Player


/**
 * @author jinguochong
 * @since  2021/2/24
 */
class H264PlayerActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private var h264Player: H264Player? = null
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aty_play_h264)
        initSurface()

        val playBtn: Button = findViewById(R.id.btn_play_h264)
        playBtn.setOnClickListener {
            h264Player = H264Player(
                Environment.getExternalStorageDirectory().absolutePath + "/test.h264",
                surfaceHolder.surface
            )
            h264Player!!.play()
        }
    }

    private fun initSurface() {
        val surfaceView: SurfaceView = findViewById(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }


}