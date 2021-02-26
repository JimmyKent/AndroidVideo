package com.phatty.androidvideo

import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.phatty.androidvideo.muxer.Mp4Player

class PlayMp4Activity : AppCompatActivity(), SurfaceHolder.Callback {
    private val mMp4Player: Mp4Player = Mp4Player()
    private lateinit var surfaceHolder: SurfaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_mp4)
        initSurface()

        val playBtn: Button = findViewById(R.id.btn_play_mp4)
        mMp4Player.setDataSource(Environment.getExternalStorageDirectory().absolutePath + "/aaa.mp4")
        playBtn.setOnClickListener {
            mMp4Player.start()
        }
    }

    private fun initSurface() {
        val surfaceView: SurfaceView = findViewById(R.id.surface_view)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mMp4Player.setSurface(surfaceHolder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }


}