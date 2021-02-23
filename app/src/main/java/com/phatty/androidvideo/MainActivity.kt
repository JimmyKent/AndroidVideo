package com.phatty.androidvideo

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var mImageView: ImageView
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mCustomView: CustomShowPicture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mImageView = findViewById(R.id.iv)
        mSurfaceView = findViewById(R.id.surface_view)
        mCustomView = findViewById(R.id.custom_view)

        mImageView.setBackgroundResource(R.drawable.ic_launcher)

        mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                val paint = Paint()
                paint.isAntiAlias = true
                paint.style = Paint.Style.STROKE
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
                val canvas: Canvas = surfaceHolder.lockCanvas() // 先锁定当前surfaceView的画布
                canvas.drawBitmap(bitmap, 0f, 0f, paint) // 执行绘制操作
                surfaceHolder.unlockCanvasAndPost(canvas) // 解除锁定并显示在界面上
            }

            override fun surfaceChanged(
                surfaceHolder: SurfaceHolder,
                i: Int,
                i1: Int,
                i2: Int
            ) {
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {}
        })
    }

}