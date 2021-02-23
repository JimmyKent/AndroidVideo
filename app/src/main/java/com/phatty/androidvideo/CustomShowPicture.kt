package com.phatty.androidvideo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * @author jinguochong
 * @since  2021/2/23
 */
class CustomShowPicture : View {

    private val paint = Paint()
    private lateinit var bitmap: Bitmap

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init()
    }


    private fun init() {
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(bitmap, 0f, 0f, paint)
    }
}